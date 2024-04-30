package org.moera.node.rest.task;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.api.node.NodeApiException;
import org.moera.node.api.node.NodeApiNotFoundException;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.RemoteUserListItem;
import org.moera.node.data.RemoteUserListItemRepository;
import org.moera.node.data.UserList;
import org.moera.node.operations.UserListOperations;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteUserListItemFetchTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(RemoteUserListItemFetchTask.class);

    private static final int MAX_RETRIES = 5;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(10);

    private static final int MAX_UPDATE_RETRIES = 3;
    private static final Duration UPDATE_RETRY_DELAY = Duration.ofSeconds(10);

    private final String sheriffName;
    private final Entry entry;

    @Inject
    private RemoteUserListItemRepository remoteUserListItemRepository;

    @Inject
    private EntryRepository entryRepository;

    public RemoteUserListItemFetchTask(String sheriffName, Entry entry) {
        this.sheriffName = sheriffName;
        this.entry = entry;
    }

    @Override
    protected void execute() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                fetch();
                return;
            } catch (Exception e) {
                log.error("Error fetching user list item {}/{}/{}: {}",
                        sheriffName, UserList.SHERIFF_HIDE, entry.getOwnerName(), e.getMessage());
            }
            try {
                Thread.sleep(RETRY_DELAY.toMillis());
            } catch (InterruptedException e) {
                // ignore
            }
        }
        log.error("Reached max number of retries of fetching user list item {}/{}/{}, giving up",
                sheriffName, UserList.SHERIFF_HIDE, entry.getOwnerName());
    }

    private void fetch() throws NodeApiException {
        boolean absent;
        try {
            absent = nodeApi.getUserListItem(sheriffName, UserList.SHERIFF_HIDE, entry.getOwnerName()) == null;
        } catch (NodeApiNotFoundException e) {
            absent = true;
        }
        save(absent);
        if (!absent) {
            updateEntry();
        }
    }

    private void save(boolean absent) {
        tx.executeWriteQuietly(() -> { // If exception occurs, it means the record is already saved by a parallel task
            RemoteUserListItem item = new RemoteUserListItem();
            item.setId(UUID.randomUUID());
            item.setNodeId(getNodeId());
            item.setListNodeName(sheriffName);
            item.setListName(UserList.SHERIFF_HIDE);
            item.setNodeName(entry.getOwnerName());
            item.setAbsent(absent);
            Duration ttl = item.isAbsent() ? UserListOperations.ABSENT_TTL : UserListOperations.PRESENT_TTL;
            item.setDeadline(Timestamp.from(Instant.now().plus(ttl)));
            remoteUserListItemRepository.save(item);
        });
    }

    private void updateEntry() {
        for (int i = 0; i < MAX_UPDATE_RETRIES; i++) {
            Boolean success = tx.executeWriteQuietly(() -> {
                Entry liveEntry = entryRepository.findByNodeIdAndId(universalContext.nodeId(), entry.getId())
                        .orElse(null);
                if (liveEntry == null) {
                    return false;
                }
                liveEntry.setSheriffUserListReferred(true);
                return true;
            });

            if (Boolean.TRUE.equals(success)) {
                return;
            }

            try {
                // The entry may not be saved yet, let's wait a little
                Thread.sleep(UPDATE_RETRY_DELAY.toMillis());
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

}
