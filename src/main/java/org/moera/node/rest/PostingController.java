package org.moera.node.rest;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingText;
import org.moera.node.option.Options;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/postings")
public class PostingController {

    private static Logger log = LoggerFactory.getLogger(PostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private PostingRepository postingRepository;

    private AtomicInteger nonce = new AtomicInteger(0);

    @PostMapping
    @Admin
    @ResponseBody
    @Transactional
    public PostingInfo post(@Valid @RequestBody PostingText postingText) {
        log.info("POST /postings (bodySrc = {}, bodyHtml = {})",
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(postingText.getBodyHtml(), 64));

        Options options = requestContext.getOptions();
        String name = options.getString("profile.registered-name");
        Integer generation = options.getInt("profile.registered-name.generation");
        if (name == null || generation == null) {
            throw new OperationFailure("posting.registered-name-not-set");
        }

        Posting posting = new Posting();
        posting.setId(UUID.randomUUID());
        posting.setNodeId(options.nodeId());
        posting.setOwnerName(name);
        posting.setOwnerGeneration(generation);
        postingText.toPosting(posting);
        posting.setMoment(buildMoment(posting.getCreated()));
        postingRepository.saveAndFlush(posting);

        return new PostingInfo(posting);
    }

    private long buildMoment(Timestamp timestamp) {
        return Util.toEpochSecond(timestamp) * 100 + nonce.getAndIncrement() % 100;
    }

}
