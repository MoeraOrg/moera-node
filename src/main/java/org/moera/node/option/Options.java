package org.moera.node.option;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.moera.lib.node.types.SettingDescriptor;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Option;
import org.moera.node.data.OptionRepository;
import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.TransactionAbsentException;
import org.moera.node.option.type.OptionTypeBase;
import org.moera.node.util.ExtendedDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

public class Options {

    private static final Logger log = LoggerFactory.getLogger(Options.class);

    private static final String ENCRYPTION_TAG = "enc:";

    private final Map<String, Object> values = new HashMap<>();
    private final ReadWriteLock valuesLock = new ReentrantReadWriteLock();
    private final ThreadLocal<Map<String, Object>> transaction = new ThreadLocal<>();
    private final ThreadLocal<Integer> transactionDepth = new ThreadLocal<>();

    private final UUID nodeId;
    private final OptionsMetadata optionsMetadata;
    private final OptionRepository optionRepository;
    private final OptionHookManager optionHookManager;
    private boolean loading;

    public Options(UUID nodeId, OptionsMetadata optionsMetadata, OptionRepository optionRepository,
                   OptionHookManager optionHookManager) {
        this.nodeId = nodeId;
        this.optionsMetadata = optionsMetadata;
        this.optionRepository = optionRepository;
        this.optionHookManager = optionHookManager;

        load();
    }

    private void load() {
        loading = true;
        try {
            optionsMetadata.getDescriptorsForNode(nodeId).stream()
                    .filter(desc -> desc.getDefaultValue() != null)
                    .forEach(desc -> loadValue(desc.getName(), desc.getDefaultValue()));
            optionRepository.findAllByNodeId(nodeId).forEach(option -> loadValue(option.getName(), option.getValue()));
        } finally {
            loading = false;
        }
    }

    public void reload() {
        valuesLock.writeLock().lock();
        try {
            load();
        } finally {
            valuesLock.writeLock().unlock();
        }
    }

    private void lockRead() {
        if (!inTransaction()) {
            valuesLock.readLock().lock();
        }
    }

    private void unlockRead() {
        if (!inTransaction()) {
            valuesLock.readLock().unlock();
        }
    }

    private void lockWrite() {
        if (!inTransaction()) {
            valuesLock.writeLock().lock();
        }
    }

    private void unlockWrite() {
        if (!inTransaction()) {
            valuesLock.writeLock().unlock();
        }
    }

    private boolean inTransaction() {
        return transaction.get() != null;
    }

    private void beginTransaction() {
        if (inTransaction()) {
            transactionDepth.set(transactionDepth.get() + 1);
            return;
        }
        lockWrite();
        transaction.set(new HashMap<>());
        transactionDepth.set(1);
    }

    private void commit() {
        if (!inTransaction()) {
            throw new TransactionAbsentException();
        }
        if (transactionDepth.get() > 1) {
            transactionDepth.set(transactionDepth.get() - 1);
            return;
        }

        List<OptionValueChange> changes = transaction.get().entrySet().stream()
                .map(update ->
                        new OptionValueChange(nodeId, update.getKey(), values.get(update.getKey()), update.getValue()))
                .filter(OptionValueChange::isTangible)
                .toList();

        values.putAll(transaction.get());
        transaction.remove();
        transactionDepth.remove();
        unlockWrite();

        changes.forEach(optionHookManager::invoke);
    }

    private void rollback() {
        if (!inTransaction()) {
            throw new TransactionAbsentException();
        }
        if (transactionDepth.get() > 1) {
            transactionDepth.set(transactionDepth.get() - 1);
            return;
        }
        transaction.remove();
        transactionDepth.remove();
        unlockWrite();
    }

    public void runInTransaction(Consumer<Options> operation) {
        beginTransaction();
        try {
            operation.accept(this);
        } catch (Throwable t) {
            rollback();
            throw t;
        }
        commit();
    }

    private Object transactionalGet(String name) {
        if (inTransaction()) {
            if (transaction.get().containsKey(name)) {
                return transaction.get().get(name);
            } else {
                return values.get(name);
            }
        } else {
            return values.get(name);
        }
    }

    private void transactionalPut(String name, Object value) {
        if (inTransaction()) {
            transaction.get().put(name, value);
        } else {
            OptionValueChange change = new OptionValueChange(nodeId, name, values.get(name), value);
            values.put(name, value);
            if (!loading && change.isTangible()) {
                optionHookManager.invoke(change);
            }
        }
    }

    private String encryptValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return value;
        }

        SecretKey key = optionsMetadata.getEncryptionKey();
        if (key == null) {
            log.warn("Encryption key is not set, saving plain text");
            return value;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            return ENCRYPTION_TAG + Base64.getEncoder().encodeToString(encryptedValue);
        } catch (GeneralSecurityException e) {
            log.error("Encryption error", e);
            return value;
        }
    }

    private String decryptValue(String value) {
        if (ObjectUtils.isEmpty(value) || !value.startsWith(ENCRYPTION_TAG)) {
            return value;
        }

        SecretKey key = optionsMetadata.getEncryptionKey();
        if (key == null) {
            log.error("Encryption key is not set, cannot decrypt");
            return value;
        }

        try {
            byte[] encryptedValue = Base64.getDecoder().decode(value.substring(ENCRYPTION_TAG.length()));

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedValue = cipher.doFinal(encryptedValue);

            return new String(decryptedValue, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    private String serializeValue(String type, Object value) {
        if (value == null) {
            return null;
        }
        return optionsMetadata.getType(type).serializeValue(value);
    }

    private Object deserializeValue(String type, String value) {
        if (value == null) {
            return null;
        }
        return optionsMetadata.getType(type).deserializeValue(value);
    }

    private void loadValue(String name, String value) {
        SettingDescriptor desc = optionsMetadata.getDescriptor(name);
        if (desc == null) {
            log.warn("No metadata for option {}", name);
            return;
        }

        try {
            if (Boolean.TRUE.equals(desc.getEncrypted())) {
                value = decryptValue(value);
            }
            transactionalPut(name, deserializeValue(desc.getType(), value));
        } catch (DeserializeOptionValueException e) {
            log.error("{}: {}", e.getMessage(), name);
        }
    }

    private <T> T forName(String name, OptionMapper<T> mapper) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return null;
        }
        lockRead();
        try {
            return mapper.map(transactionalGet(name), optionType);
        } finally {
            unlockRead();
        }
    }

    private Object get(String name) {
        return forName(name, (value, optionType) -> value);
    }

    public String getString(String name) {
        return forName(name, (value, optionType) -> optionType.getString(value));
    }

    public Boolean getBool(String name) {
        return forName(name, (value, optionType) -> optionType.getBool(value));
    }

    public Integer getInt(String name) {
        return forName(name,
                (value, optionType) -> optionType.getInt(value, optionsMetadata.getOptionTypeModifiers(name)));
    }

    public Long getLong(String name) {
        return forName(name, (value, optionType) -> optionType.getLong(value));
    }

    public PrivateKey getPrivateKey(String name) {
        return forName(name, (value, optionType) -> optionType.getPrivateKey(value));
    }

    public PublicKey getPublicKey(String name) {
        return forName(name, (value, optionType) -> optionType.getPublicKey(value));
    }

    public ExtendedDuration getDuration(String name) {
        return forName(name, (value, optionType) -> optionType.getDuration(value));
    }

    public UUID getUuid(String name) {
        return forName(name, (value, optionType) -> optionType.getUuid(value));
    }

    public Timestamp getTimestamp(String name) {
        return forName(name, (value, optionType) -> optionType.getTimestamp(value));
    }

    public Principal getPrincipal(String name) {
        return forName(name, (value, optionType) -> optionType.getPrincipal(value));
    }

    // Returns only committed values of non-internal options
    public void forEach(OptionConsumer consumer) {
        lockRead();
        try {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (optionsMetadata.isInternal(entry.getKey())) {
                    continue;
                }
                OptionTypeBase optionType = optionsMetadata.getOptionType(entry.getKey());
                if (optionType == null) {
                    continue;
                }
                try {
                    consumer.consume(entry.getKey(), entry.getValue(), optionType);
                } catch (Exception e) {
                    // ignore
                }
            }
        } finally {
            unlockRead();
        }
    }

    public void set(String name, Object value) {
        OptionTypeBase optionType = optionsMetadata.getOptionType(name);
        if (optionType == null) {
            return;
        }

        Object newValue = optionType.accept(value, optionsMetadata.getOptionTypeModifiers(name));

        lockWrite();
        try {
            Option option = optionRepository.findByNodeIdAndName(nodeId, name).orElse(new Option(nodeId, name));
            String serializedValue = serializeValue(optionType.getTypeName(), newValue);
            if (optionsMetadata.isEncrypted(name)) {
                serializedValue = encryptValue(serializedValue);
            }
            option.setValue(serializedValue);
            optionRepository.saveAndFlush(option);

            transactionalPut(name, newValue);
        } finally {
            unlockWrite();
        }
    }

    public void reset(String name) {
        lockWrite();
        try {
            optionRepository.deleteByNodeIdAndName(nodeId, name);
            SettingDescriptor desc = optionsMetadata.getDescriptor(name);
            transactionalPut(name, deserializeValue(desc.getType(), desc.getDefaultValue()));
        } finally {
            unlockWrite();
        }
    }

    public void resave(String name) {
        set(name, get(name));
    }

    public UUID nodeId() {
        return nodeId;
    }

    public String nodeName() {
        return getString("profile.node-name");
    }

    public boolean isFrozen() {
        return getBool("frozen");
    }

}
