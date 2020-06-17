package org.moera.node.rest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.node.auth.Admin;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ActivityReactionFilter;
import org.moera.node.model.ActivityReactionInfo;
import org.moera.node.model.RemotePosting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/activity/reactions")
public class ActivityReactionController {

    private static Logger log = LoggerFactory.getLogger(ActivityReactionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @PostMapping
    @Admin
    public List<ActivityReactionInfo> getAll(@Valid @RequestBody ActivityReactionFilter filter) {
        log.info("GET /activity/reactions");

        if (filter.getPostings() == null || filter.getPostings().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> remotePostingIds = filter.getPostings().stream()
                .map(RemotePosting::getPostingId)
                .collect(Collectors.toList());
        List<OwnReaction> ownReactions = ownReactionRepository.findAllByRemotePostingIds(
                requestContext.nodeId(), remotePostingIds);

        return ownReactions.stream()
                .filter(r -> filter.getPostings().contains(r.getRemotePosting()))
                .map(ActivityReactionInfo::new)
                .collect(Collectors.toList());
    }

}
