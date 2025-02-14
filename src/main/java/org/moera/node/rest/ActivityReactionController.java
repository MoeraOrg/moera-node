package org.moera.node.rest;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.ActivityReactionFilter;
import org.moera.lib.node.types.ActivityReactionInfo;
import org.moera.lib.node.types.RemotePosting;
import org.moera.lib.node.types.Scope;
import org.moera.node.auth.Admin;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ActivityReactionInfoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/activity/reactions")
@NoCache
public class ActivityReactionController {

    private static final Logger log = LoggerFactory.getLogger(ActivityReactionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @PostMapping("/search")
    @Admin(Scope.VIEW_CONTENT)
    @Transactional
    public List<ActivityReactionInfo> search(@RequestBody ActivityReactionFilter filter) {
        log.info("POST /activity/reactions/search");

        if (filter.getPostings() == null || filter.getPostings().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> remotePostingIds = filter.getPostings().stream()
            .map(RemotePosting::getPostingId)
            .collect(Collectors.toList());
        List<OwnReaction> ownReactions = ownReactionRepository.findAllByRemotePostingIds(
            requestContext.nodeId(), remotePostingIds
        );

        Set<RemotePosting> postingSet = new HashSet<>(filter.getPostings());
        return ownReactions.stream()
            .filter(r -> postingSet.contains(r.getRemotePosting()))
            .map(ActivityReactionInfoUtil::build)
            .collect(Collectors.toList());
    }

}
