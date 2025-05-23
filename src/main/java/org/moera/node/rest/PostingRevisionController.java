package org.moera.node.rest;

import java.util.Optional;
import java.util.UUID;

import org.moera.lib.node.types.Scope;
import org.moera.node.data.EntryRevision;
import org.moera.node.data.Posting;
import org.moera.node.global.ApiController;
import org.moera.node.liberin.Liberin;
import org.moera.node.liberin.model.PostingUpdatedLiberin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/postings/{postingId}/revisions")
public class PostingRevisionController extends PostingRevisionControllerBase {

    private static final Logger log = LoggerFactory.getLogger(PostingRevisionController.class);

    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected String getDirectory() {
        return "/postings";
    }

    @Override
    protected Optional<Posting> findPosting(UUID postingId) {
        return postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId);
    }

    @Override
    protected boolean isViewPermitted(Posting posting) {
        return requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT);
    }

    @Override
    protected Optional<EntryRevision> findRevision(UUID postingId, UUID id) {
        return entryRevisionRepository.findByEntryIdAndId(requestContext.nodeId(), postingId, id);
    }

    @Override
    protected Optional<EntryRevision> findRevisionWithAttachments(UUID postingId, UUID id) {
        return entryRevisionRepository.findWithAttachmentsByEntryIdAndId(requestContext.nodeId(), postingId, id);
    }

    @Override
    protected Liberin getRestorationLiberin(Posting posting, EntryRevision latest) {
        return new PostingUpdatedLiberin(posting, latest, posting.getViewE());
    }

}
