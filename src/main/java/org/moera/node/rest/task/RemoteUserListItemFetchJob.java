package org.moera.node.rest.task;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.RemoteUserListItem;
import org.moera.node.data.RemoteUserListItemRepository;
import org.moera.node.data.UserList;
import org.moera.node.operations.UserListOperations;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteUserListItemFetchJob
        extends Job<RemoteUserListItemFetchJob.Parameters, RemoteUserListItemFetchJob.State> {

    public static class Parameters {

        private String sheriffName;
        private String ownerName;
        private UUID entryId;

        public Parameters() {
        }

        public Parameters(String sheriffName, String ownerName, UUID entryId) {
            this.sheriffName = sheriffName;
            this.ownerName = ownerName;
            this.entryId = entryId;
        }

        public String getSheriffName() {
            return sheriffName;
        }

        public void setSheriffName(String sheriffName) {
            this.sheriffName = sheriffName;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public UUID getEntryId() {
            return entryId;
        }

        public void setEntryId(UUID entryId) {
            this.entryId = entryId;
        }

    }

    public static class State {

        private Boolean absent = null;
        private boolean saved;

        public State() {
        }

        public Boolean getAbsent() {
            return absent;
        }

        public void setAbsent(Boolean absent) {
            this.absent = absent;
        }

        public boolean isSaved() {
            return saved;
        }

        public void setSaved(boolean saved) {
            this.saved = saved;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(RemoteUserListItemFetchJob.class);

    @Inject
    private RemoteUserListItemRepository remoteUserListItemRepository;

    @Inject
    private EntryRepository entryRepository;

    public RemoteUserListItemFetchJob() {
        state = new State();
        retryCount(5, "PT10S");
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void started() {
        super.started();
        log.info("Fetching user list item {}/{}/{}, entry {}",
                parameters.sheriffName, UserList.SHERIFF_HIDE, parameters.ownerName,
                LogUtil.format(parameters.entryId));
    }

    @Override
    protected void execute() throws MoeraNodeException {
        if (state.absent == null) {
            try {
                state.absent = nodeApi
                    .at(parameters.sheriffName)
                    .getUserListItem(UserList.SHERIFF_HIDE, parameters.ownerName) == null;
            } catch (MoeraNodeApiNotFoundException e) {
                state.absent = true;
            }
            checkpoint();
        }

        if (!state.saved) {
            save();
            state.saved = true;
            checkpoint();
        }

        if (!state.absent) {
            updateEntry();
        }
    }

    private void save() {
        tx.executeWriteQuietly(() -> {
            // If an exception occurs, it means the record is already saved by a parallel task
            RemoteUserListItem item = new RemoteUserListItem();
            item.setId(UUID.randomUUID());
            item.setNodeId(getNodeId());
            item.setListNodeName(parameters.sheriffName);
            item.setListName(UserList.SHERIFF_HIDE);
            item.setNodeName(parameters.ownerName);
            item.setAbsent(state.absent);
            Duration ttl = item.isAbsent() ? UserListOperations.ABSENT_TTL : UserListOperations.PRESENT_TTL;
            item.setDeadline(Timestamp.from(Instant.now().plus(ttl)));
            remoteUserListItemRepository.save(item);
        });
    }

    private void updateEntry() {
        Boolean success = tx.executeWriteQuietly(() -> {
            Entry liveEntry = entryRepository.findByNodeIdAndId(universalContext.nodeId(), parameters.entryId)
                    .orElse(null);
            if (liveEntry == null) {
                return false;
            }
            liveEntry.setSheriffUserListReferred(true);
            return true;
        });

        if (!Boolean.TRUE.equals(success)) {
            retry();
        }
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info("Fetched user list item {}/{}/{}, entry {}: {}",
                parameters.sheriffName, UserList.SHERIFF_HIDE, parameters.ownerName,
                LogUtil.format(parameters.entryId), state.absent ? "absent" : "present");
    }

}
