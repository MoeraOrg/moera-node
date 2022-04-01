package org.moera.node.rest;

import java.util.UUID;

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
    protected Posting findPosting(UUID postingId) {
        return postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
    }

    @Override
    protected EntryRevision findRevision(UUID postingId, UUID id) {
        return entryRevisionRepository.findByEntryIdAndId(requestContext.nodeId(), postingId, id).orElse(null);
    }

    @Override
    protected Liberin getRestorationLiberin(Posting posting) {
        return new PostingUpdatedLiberin(posting);
    }

}
