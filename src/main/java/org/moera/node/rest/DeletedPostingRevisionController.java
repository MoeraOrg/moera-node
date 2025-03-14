package org.moera.node.rest;

import java.util.Optional;
import java.util.UUID;

import org.moera.lib.node.types.Scope;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.global.ApiController;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/deleted-postings/{postingId}/revisions")
public class DeletedPostingRevisionController extends PostingRevisionControllerBase {

    private static final Logger log = LoggerFactory.getLogger(DeletedPostingRevisionController.class);

    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected String getDirectory() {
        return "/deleted-postings";
    }

    @Override
    protected Optional<Posting> findPosting(UUID postingId) {
        return postingRepository.findDeletedById(requestContext.nodeId(), postingId);
    }

    @Override
    protected boolean isViewPermitted(Posting posting) {
        return requestContext.isAdmin(Scope.DELETE_OWN_CONTENT);
    }

    @Override
    protected Optional<EntryRevision> findRevision(UUID postingId, UUID id) {
        return entryRevisionRepository.findByDeletedEntryIdAndId(requestContext.nodeId(), postingId, id);
    }

    @Override
    protected Optional<EntryRevision> findRevisionWithAttachments(UUID postingId, UUID id) {
        return entryRevisionRepository.findWithAttachmentsByDeletedEntryIdAndId(requestContext.nodeId(), postingId, id);
    }

    @Override
    protected Liberin getRestorationLiberin(Posting posting, EntryRevision latest) {
        return new PostingRestoredLiberin(posting);
    }

}
