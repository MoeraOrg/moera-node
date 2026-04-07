package org.moera.node.userlist;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.node.data.RemoteUserListItem;
import org.moera.node.data.RemoteUserListItemRepository;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RemoteUserListItemFetchJob
    <P extends RemoteUserListItemFetchJob.Parameters, S extends RemoteUserListItemFetchJob.State> extends Job<P, S> {

    public static class Parameters {

        protected String listNodeName;
        protected String listName;
        protected String ownerName;

        protected Parameters() {
        }

        public Parameters(String listNodeName, String listName, String ownerName) {
            this.listNodeName = listNodeName;
            this.listName = listName;
            this.ownerName = ownerName;
        }

        public String getListNodeName() {
            return listNodeName;
        }

        public void setListNodeName(String listNodeName) {
            this.listNodeName = listNodeName;
        }

        public String getListName() {
            return listName;
        }

        public void setListName(String listName) {
            this.listName = listName;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

    }

    public static class State {

        protected Boolean absent = null;
        protected boolean saved;

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

    public RemoteUserListItemFetchJob() {
        retryCount(5, "PT10S");
    }

    @Override
    protected void started() {
        super.started();
        log.info(
            "Fetching user list item {}/{}/{}",
            parameters.listNodeName, parameters.listName, parameters.ownerName
        );
    }

    @Override
    protected void execute() throws MoeraNodeException {
        if (state.absent == null) {
            try {
                state.absent = nodeApi
                    .at(parameters.listNodeName)
                    .getUserListItem(parameters.listName, parameters.ownerName) == null;
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
            itemPresent();
        }
    }

    private void save() {
        tx.executeWriteQuietly(() -> {
            // If an exception occurs, it means the record is already saved by a parallel task
            RemoteUserListItem item = new RemoteUserListItem();
            item.setId(UUID.randomUUID());
            item.setNodeId(getNodeId());
            item.setListNodeName(parameters.listNodeName);
            item.setListName(parameters.listName);
            item.setNodeName(parameters.ownerName);
            item.setAbsent(state.absent);
            Duration ttl = item.isAbsent() ? UserListOperations.ABSENT_TTL : UserListOperations.PRESENT_TTL;
            item.setDeadline(Timestamp.from(Instant.now().plus(ttl)));
            remoteUserListItemRepository.save(item);
        });
    }

    private void itemPresent() {
        Boolean success = tx.executeWriteQuietly(this::updateObjects);

        if (!Boolean.TRUE.equals(success)) {
            retry();
        }
    }

    protected abstract boolean updateObjects();

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info(
            "Fetched user list item {}/{}/{}: {}",
            parameters.listNodeName,
            parameters.listName,
            parameters.ownerName,
            state.absent ? "absent" : "present"
        );
    }

}
