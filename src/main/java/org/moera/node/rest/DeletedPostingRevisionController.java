package org.moera.node.rest;

import java.util.UUID;

import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.PostingRestoredEvent;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/deleted-postings/{postingId}/revisions")
@Admin
public class DeletedPostingRevisionController extends PostingRevisionControllerBase {

    private static Logger log = LoggerFactory.getLogger(DeletedPostingRevisionController.class);

    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected String getDirectory() {
        return "/deleted-postings";
    }

    @Override
    protected Posting findPosting(UUID postingId) {
        return postingRepository.findDeletedById(requestContext.nodeId(), postingId).orElse(null);
    }

    @Override
    protected EntryRevision findRevision(UUID postingId, UUID id) {
        return entryRevisionRepository.findByDeletedEntryIdAndId(requestContext.nodeId(), postingId, id).orElse(null);
    }

    @Override
    protected Event getRestorationEvent(Posting posting) {
        return new PostingRestoredEvent(posting);
    }

}
