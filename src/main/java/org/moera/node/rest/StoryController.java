package org.moera.node.rest;

import java.util.UUID;

import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.StoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/stories")
public class StoryController {

    private static Logger log = LoggerFactory.getLogger(StoryController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @GetMapping("/{id}")
    public StoryInfo get(@PathVariable UUID id) {
        log.info("GET /stories/{id}, (id = {})", LogUtil.format(id));

        Story story = storyRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (story == null) {
            throw new ObjectNotFoundFailure("story.not-found");
        }

        return new StoryInfo(story, requestContext.isAdmin());
    }

}
