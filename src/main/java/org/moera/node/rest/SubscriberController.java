package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriberRepository;
import org.moera.node.data.SubscriptionType;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.SubscriberDescription;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/subscribers")
public class SubscriberController {

    private static Logger log = LoggerFactory.getLogger(SubscriberController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private SubscriberRepository subscriberRepository;

    @Inject
    private PostingRepository postingRepository;

    @PostMapping
    @Transactional
    public SubscriberInfo post(@Valid @RequestBody SubscriberDescription subscriberDescription)
            throws AuthenticationException {

        log.info("POST /subscribers (subscriptionType = {}, feedName = {}, postingId = {})",
                LogUtil.format(subscriberDescription.getType()),
                LogUtil.format(subscriberDescription.getFeedName()),
                LogUtil.format(subscriberDescription.getPostingId()));

        SubscriptionType type = SubscriptionType.forValue(subscriberDescription.getType());
        if (type == null) {
            throw new ValidationFailure("subscriberDescription.subscriptionType.unknown");
        }
        String ownerName = requestContext.getClientName();
        if (StringUtils.isEmpty(ownerName)) {
            throw new AuthenticationException();
        }
        if (similarExists(type, subscriberDescription)) {
            throw new OperationFailure("subscriber.already-exists");
        }
        validate(type, subscriberDescription);

        Subscriber subscriber = new Subscriber();
        subscriber.setId(UUID.randomUUID());
        subscriber.setNodeId(requestContext.nodeId());
        subscriber.setSubscriptionType(type);
        subscriber.setRemoteNodeName(ownerName);
        if (!StringUtils.isEmpty(subscriberDescription.getFeedName())) {
            if (!Feed.isStandard(subscriberDescription.getFeedName())) {
                throw new ValidationFailure("subscriberDescription.feedName.not-found");
            }
            // TODO check permissions
            subscriber.setFeedName(subscriberDescription.getFeedName());
        }
        if (subscriberDescription.getPostingId() != null) {
            Posting posting = postingRepository.findByNodeIdAndId(
                    requestContext.nodeId(), subscriberDescription.getPostingId())
                    .orElse(null);
            if (posting == null) {
                throw new ValidationFailure("subscriberDescription.postingId.not-found");
            }
            subscriber.setEntry(posting);
        }
        subscriber = subscriberRepository.save(subscriber);

        // TODO event
        return new SubscriberInfo(subscriber);
    }

    private boolean similarExists(SubscriptionType type, SubscriberDescription description) {
        switch (type) {
            case FEED:
                return subscriberRepository.countByFeedName(requestContext.nodeId(), requestContext.getClientName(),
                        type, description.getFeedName()) > 0;
            case POSTING:
                return subscriberRepository.countByEntryId(requestContext.nodeId(), requestContext.getClientName(),
                        type, description.getPostingId()) > 0;
        }
        return false; // Should never be reached
    }

    private void validate(SubscriptionType type, SubscriberDescription description) {
        switch (type) {
            case FEED:
                if (StringUtils.isEmpty(description.getFeedName())) {
                    throw new ValidationFailure("subscriberDescription.feedName.blank");
                }
                break;
            case POSTING:
                if (description.getPostingId() == null) {
                    throw new ValidationFailure("subscriberDescription.postingId.blank");
                }
                break;
        }
    }

}
