package org.moera.node.rest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.RootAdmin;
import org.moera.node.data.OptionDefault;
import org.moera.node.data.OptionDefaultRepository;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.SettingInfo;
import org.moera.node.model.SettingMetaAttributes;
import org.moera.node.model.SettingMetaInfo;
import org.moera.node.model.event.ClientSettingsChangedEvent;
import org.moera.node.model.event.NodeSettingsChangedEvent;
import org.moera.node.model.event.NodeSettingsMetaChangedEvent;
import org.moera.node.option.OptionDescriptor;
import org.moera.node.option.OptionsMetadata;
import org.moera.node.option.type.OptionTypeBase;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/settings")
@NoCache
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    private OptionsMetadata optionsMetadata;

    @Inject
    private OptionDefaultRepository optionDefaultRepository;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private EventManager eventManager;

    private List<SettingInfo> getOptions(Predicate<String> nameFilter) {
        List<SettingInfo> list = new ArrayList<>();
        requestContext.getOptions().forEach((name, value, optionType) -> {
            if (!nameFilter.test(name)) {
                return;
            }
            list.add(new SettingInfo(name, optionType.getString(value)));
        });
        list.sort(Comparator.comparing(SettingInfo::getName));

        return list;
    }

    @GetMapping("/node")
    @Admin
    public List<SettingInfo> getForNode(@RequestParam(required = false) String prefix) {
        log.info("GET /settings/node");

        return getOptions(name -> !name.startsWith(OptionsMetadata.CLIENT_PREFIX)
                && (prefix == null || name.startsWith(prefix)));
    }

    @GetMapping("/client")
    @Admin
    public List<SettingInfo> getForClient(@RequestParam(required = false) String prefix) {
        log.info("GET /settings/client");

        return getOptions(name -> name.startsWith(OptionsMetadata.CLIENT_PREFIX)
                && (prefix == null || name.startsWith(prefix)));
    }

    @GetMapping("/node/metadata")
    @Admin
    public List<SettingMetaInfo> getMetadata(@RequestParam(required = false) String prefix) {
        log.info("GET /settings/node/metadata");

        return optionsMetadata.getDescriptors().values().stream()
                .filter(d -> !d.isInternal())
                .filter(d -> prefix == null || d.getName().startsWith(prefix))
                .map(SettingMetaInfo::new)
                .sorted(Comparator.comparing(SettingMetaInfo::getName))
                .collect(Collectors.toList());
    }

    @PutMapping("/node/metadata")
    @RootAdmin
    @Transactional
    public Result putMetadata(@RequestBody @Valid List<SettingMetaAttributes> metaAttributes) throws Throwable {
        log.info("PUT /settings/node/metadata");

        boolean metaChanged = Transaction.execute(txManager, () -> {
            boolean changed = false;

            for (SettingMetaAttributes meta : metaAttributes) {
                OptionDescriptor descriptor = optionsMetadata.getDescriptor(meta.getName());
                if (descriptor == null) {
                    throw new OperationFailure("setting.unknown");
                }
                if (descriptor.isInternal()) {
                    throw new OperationFailure("setting.internal");
                }

                OptionTypeBase optionType = optionsMetadata.getOptionType(meta.getName());
                if (optionType == null) {
                    continue;
                }
                Object newValue = optionType.accept(meta.getDefaultValue(),
                        optionsMetadata.getOptionTypeModifiers(meta.getName()));

                OptionDefault optionDefault = optionDefaultRepository.findByName(meta.getName())
                        .orElse(new OptionDefault(meta.getName()));
                if (newValue != null) {
                    if (optionDefault.getId() == null) {
                        optionDefault.setId(UUID.randomUUID());
                    }
                    optionDefault.setValue(optionType.serializeValue(newValue));
                    optionDefault.setPrivileged(meta.getPrivileged());
                    optionDefaultRepository.save(optionDefault);
                } else if (optionDefault.getId() != null) {
                    optionDefaultRepository.delete(optionDefault);
                }
                changed = true;
            }

            return changed;
        });

        if (metaChanged) {
            optionsMetadata.reload();
            domains.getAllDomainNames().stream()
                    .map(domains::getDomainOptions)
                    .forEach(options -> {
                        options.reload();
                        eventManager.send(options.nodeId(), new NodeSettingsMetaChangedEvent());
                        eventManager.send(options.nodeId(), new NodeSettingsChangedEvent());
                    });
        }

        return Result.OK;
    }

    @PutMapping
    @Transactional
    public Result put(@RequestBody @Valid List<SettingInfo> settings) {
        log.info("PUT /settings");

        AtomicBoolean nodeChanged = new AtomicBoolean(false);
        AtomicBoolean clientChanged = new AtomicBoolean(false);
        requestContext.getOptions().runInTransaction(options -> {
            settings.forEach(setting -> {
                OptionDescriptor descriptor = optionsMetadata.getDescriptor(setting.getName());
                if (descriptor == null) {
                    throw new OperationFailure("setting.unknown");
                }
                if (descriptor.isInternal()) {
                    throw new OperationFailure("setting.internal");
                }
                if (!descriptor.isPrivileged() && !requestContext.isAdmin()
                        || descriptor.isPrivileged() && !requestContext.isRootAdmin()) {
                    throw new AuthenticationException();
                }
                if (setting.getValue() != null) {
                    options.set(setting.getName(), setting.getValue());
                } else {
                    options.reset(setting.getName());
                }
                if (setting.getName().startsWith(OptionsMetadata.CLIENT_PREFIX)) {
                    clientChanged.set(true);
                } else {
                    nodeChanged.set(true);
                }
            });
        });

        if (nodeChanged.get()) {
            requestContext.send(new NodeSettingsChangedEvent());
        }
        if (clientChanged.get()) {
            requestContext.send(new ClientSettingsChangedEvent());
        }

        return Result.OK;
    }

}
