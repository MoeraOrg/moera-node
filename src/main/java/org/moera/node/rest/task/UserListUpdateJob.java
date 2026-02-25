package org.moera.node.rest.task;

import java.util.List;
import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.UserListSliceInfo;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.RemoteUserListItem;
import org.moera.node.data.RemoteUserListItemRepository;
import org.moera.node.operations.UserListOperations;
import org.moera.node.task.Job;
import org.moera.node.util.SafeInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.ObjectMapper;

public class UserListUpdateJob extends Job<UserListUpdateJob.Parameters, UserListUpdateJob.State> {

    public static class Parameters {

        private String listNodeName;
        private String listName;
        private List<String> sheriffFeedNames;
        private String nodeName;
        private boolean delete;

        public Parameters() {
        }

        public Parameters(
            String listNodeName, String listName, List<String> sheriffFeedNames, String nodeName, boolean delete
        ) {
            this.listNodeName = listNodeName;
            this.listName = listName;
            this.sheriffFeedNames = sheriffFeedNames;
            this.nodeName = nodeName;
            this.delete = delete;
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

        public List<String> getSheriffFeedNames() {
            return sheriffFeedNames;
        }

        public void setSheriffFeedNames(List<String> sheriffFeedNames) {
            this.sheriffFeedNames = sheriffFeedNames;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public boolean isDelete() {
            return delete;
        }

        public void setDelete(boolean delete) {
            this.delete = delete;
        }

    }

    public static class State {

        private Long before;

        public State() {
        }

        public Long getBefore() {
            return before;
        }

        public void setBefore(Long before) {
            this.before = before;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(UserListUpdateJob.class);

    private static final int LIST_PAGE_SIZE = 200;

    @Inject
    private RemoteUserListItemRepository remoteUserListItemRepository;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private UserListOperations userListOperations;

    public UserListUpdateJob() {
        state = new State();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void started() {
        super.started();
        if (parameters.nodeName == null) {
            if (!parameters.delete) {
                log.info("Fetching user list {} from node {}", parameters.listName, parameters.listNodeName);
            } else {
                log.info("Deleting user list {} from node {}", parameters.listName, parameters.listNodeName);
            }
        } else {
            if (!parameters.delete) {
                log.info(
                    "Adding item {} to user list {} from node {}",
                    parameters.nodeName, parameters.listName, parameters.listNodeName
                );
            } else {
                log.info(
                    "Deleting item {} from user list {} from node {}",
                    parameters.nodeName, parameters.listName, parameters.listNodeName
                );
            }
        }
    }

    @Override
    protected void execute() throws MoeraNodeException {
        if (parameters.nodeName == null) {
            if (!parameters.delete) {
                addList();
            } else {
                deleteList();
            }
        } else {
            if (!parameters.delete) {
                addListItem();
            } else {
                deleteListItem();
            }
        }
    }

    private void addList() throws MoeraNodeException {
        if (entryRepository.countNotOwnedBy(universalContext.nodeId(), universalContext.nodeName()) == 0) {
            return;
        }

        UserListSliceInfo slice;
        if (state.before == null) {
            state.before = SafeInteger.MAX_VALUE;
        }
        do {
            slice = nodeApi.at(parameters.listNodeName).getUserListSlice(parameters.listName, null, state.before, null);
            var items = slice.getItems();
            tx.executeWrite(() ->
                items.forEach(item ->
                    userListOperations.addToList(
                        parameters.listNodeName,
                        parameters.listName,
                        item.getNodeName()
                    )
                )
            );
            state.before = slice.getAfter();
            checkpoint();
        } while (slice.getTotalInPast() > 0);
    }

    private void deleteList() {
        tx.executeWrite(
            () -> {
                Page<RemoteUserListItem> page;
                Pageable pageable = PageRequest.of(0, LIST_PAGE_SIZE, Sort.Direction.ASC, "cachedAt");
                do {
                    page = remoteUserListItemRepository.findNotAbsentByList(
                        universalContext.nodeId(), parameters.listNodeName, parameters.listName, pageable
                    );
                    page.forEach(item ->
                        userListOperations.deleteFromList(
                            parameters.listNodeName,
                            parameters.listName,
                            parameters.sheriffFeedNames,
                            item.getNodeName()
                        )
                    );
                    pageable = pageable.next();
                } while (page.hasNext());
            }
        );
    }

    private void addListItem() {
        tx.executeWrite(
            () -> userListOperations.addToList(parameters.listNodeName, parameters.listName, parameters.nodeName)
        );
    }

    private void deleteListItem() {
        tx.executeWrite(
            () -> userListOperations.deleteFromList(
                parameters.listNodeName,
                parameters.listName,
                parameters.sheriffFeedNames,
                parameters.nodeName
            )
        );
    }

}
