package org.moera.node.userlist;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.node.data.Entry;
import org.moera.node.data.EntryRepository;
import tools.jackson.databind.ObjectMapper;

public class RemoteSheriffUserListItemFetchJob
    extends RemoteUserListItemFetchJob<RemoteSheriffUserListItemFetchJob.Parameters, RemoteUserListItemFetchJob.State> {

    public static class Parameters extends RemoteUserListItemFetchJob.Parameters {

        private UUID entryId;

        public Parameters() {
        }

        public Parameters(String sheriffName, String ownerName, UUID entryId) {
            super(sheriffName, UserList.SHERIFF_HIDE, ownerName);
            this.entryId = entryId;
        }

        public UUID getEntryId() {
            return entryId;
        }

        public void setEntryId(UUID entryId) {
            this.entryId = entryId;
        }

    }

    @Inject
    private EntryRepository entryRepository;

    public RemoteSheriffUserListItemFetchJob() {
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
    protected boolean updateObjects() {
        Entry liveEntry = entryRepository.findByNodeIdAndId(universalContext.nodeId(), parameters.entryId)
            .orElse(null);
        if (liveEntry == null) {
            return false;
        }
        liveEntry.setSheriffUserListReferred(true);
        return true;
    }

}
