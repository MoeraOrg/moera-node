package org.moera.node.rest.task;

import java.time.Duration;
import java.util.List;
import javax.inject.Inject;

import org.moera.node.data.EntryRepository;
import org.moera.node.data.RemoteUserListItem;
import org.moera.node.data.RemoteUserListItemRepository;
import org.moera.node.model.UserListSliceInfo;
import org.moera.node.operations.UserListOperations;
import org.moera.node.task.Task;
import org.moera.node.util.SafeInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class UserListUpdateTask extends Task {

    private static final Logger log = LoggerFactory.getLogger(UserListUpdateTask.class);

    private static final int MAX_RETRIES = 10;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(10);
    private static final int LIST_PAGE_SIZE = 200;

    private final String listNodeName;
    private final String listName;
    private final List<String> sheriffFeedNames;
    private final String nodeName;
    private final boolean delete;

    @Inject
    private RemoteUserListItemRepository remoteUserListItemRepository;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private UserListOperations userListOperations;

    public UserListUpdateTask(String listNodeName, String listName, List<String> sheriffFeedNames, String nodeName,
                              boolean delete) {
        this.listNodeName = listNodeName;
        this.listName = listName;
        this.sheriffFeedNames = sheriffFeedNames;
        this.nodeName = nodeName;
        this.delete = delete;
    }

    @Override
    protected void execute() {
        if (nodeName == null) {
            if (!delete) {
                addList();
            } else {
                deleteList();
            }
        } else {
            updateListItem();
        }
    }

    private void addList() {
        if (entryRepository.countNotOwnedBy(universalContext.nodeId(), universalContext.nodeName()) == 0) {
            return;
        }

        UserListSliceInfo slice;
        long before = SafeInteger.MAX_VALUE;
        do {
            slice = null;
            for (int i = 0; i < MAX_RETRIES; i++) {
                try {
                    slice = nodeApi.getUserListItems(listNodeName, listName, before);
                    break;
                } catch (Exception e) {
                    log.error("Error fetching user list {} from node {}: {}", listName, listNodeName, e.getMessage());
                }
                try {
                    Thread.sleep(RETRY_DELAY.toMillis());
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            if (slice == null) {
                log.error("Reached max number of retries of fetching user list {} from node {}, giving up",
                        listName, listNodeName);
                return;
            }
            var items = slice.getItems();
            try {
                tx.executeWrite(() ->
                    items.forEach(item -> userListOperations.addToList(listNodeName, listName, item.getNodeName()))
                );
            } catch (Throwable e) {
                log.error("Error creating user list", e);
                break;
            }
            before = slice.getAfter();
        } while (slice.getTotalInPast() > 0);
    }

    private void deleteList() {
        tx.executeWriteQuietly(
            () -> {
                Page<RemoteUserListItem> page;
                Pageable pageable = PageRequest.of(0, LIST_PAGE_SIZE, Sort.Direction.ASC, "cachedAt");
                do {
                    page = remoteUserListItemRepository.findNotAbsentByList(
                            universalContext.nodeId(), listNodeName, listName, pageable);
                    page.forEach(item ->
                            userListOperations.deleteFromList(
                                    listNodeName, listName, sheriffFeedNames, item.getNodeName()));
                    pageable = pageable.next();
                } while (page.hasNext());
            },
            e -> log.error("Error deleting user list", e)
        );
    }

    private void updateListItem() {
        tx.executeWriteQuietly(
            () -> {
                if (!delete) {
                    userListOperations.addToList(listNodeName, listName, nodeName);
                } else {
                    userListOperations.deleteFromList(listNodeName, listName, sheriffFeedNames, nodeName);
                }
            },
            e -> log.error("Error updating user list item", e)
        );
    }

}
