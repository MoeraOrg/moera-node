package org.moera.node.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.global.RequestContext;
import org.moera.node.model.BodyMappingException;
import org.moera.node.model.CommentCreated;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentText;
import org.moera.node.model.FeedSliceInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.Result;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.RegisteredNameDetails;
import org.moera.node.notification.NotificationPacket;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class NodeApi {

    private static final Duration CALL_API_CONNECTION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration CALL_API_REQUEST_TIMEOUT = Duration.ofMinutes(1);

    private ThreadLocal<UUID> nodeId = new ThreadLocal<>();

    @Inject
    private RequestContext requestContext;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private NamingCache namingCache;

    public void setNodeId(UUID nodeId) {
        this.nodeId.set(nodeId);
    }

    private UUID getNodeId() {
        return nodeId.get() != null ? nodeId.get() : requestContext.nodeId();
    }

    private String fetchNodeUri(String remoteNodeName) {
        namingCache.setNodeId(getNodeId());
        RegisteredNameDetails details = namingCache.get(remoteNodeName);
        return details != null ? UriUtil.normalize(details.getNodeUri()) : null;
    }

    private <T> T call(String method, String remoteNodeName, String location, Class<T> result)
            throws NodeApiException {

        return call(method, remoteNodeName, location, null, result);
    }

    private <T> T call(String method, String remoteNodeName, String location, Object body, Class<T> result)
            throws NodeApiException {

        String nodeUri = fetchNodeUri(remoteNodeName);
        if (nodeUri == null) {
            throw new NodeApiUnknownNameException(remoteNodeName);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(nodeUri + "/api" + location))
                .timeout(CALL_API_REQUEST_TIMEOUT)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .method(method, jsonPublisher(body))
                .build();
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CALL_API_CONNECTION_TIMEOUT)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new NodeApiException(e);
        }
        switch (HttpStatus.valueOf(response.statusCode())) {
            case NOT_FOUND:
                throw new NodeApiNotFoundException(response.uri());

            case OK:
            case CREATED:
                // do nothing
                break;

            case BAD_REQUEST:
                try {
                    Result answer = jsonParse(response.body(), Result.class);
                    throw new NodeApiValidationException(answer.getErrorCode());
                } catch (BodyMappingException e) {
                    // fallthru
                }
                // fallthru

            default:
                throw new NodeApiErrorStatusException(response.statusCode(), response.body());
        }
        return jsonParse(response.body(), result);
    }

    private HttpRequest.BodyPublisher jsonPublisher(Object body) throws NodeApiException {
        if (body == null) {
            return HttpRequest.BodyPublishers.ofString("{}");
        }
        try {
            return HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new NodeApiException("Error converting to JSON", e);
        }
    }

    private <T> T jsonParse(String data, Class<T> klass) {
        try {
            return objectMapper.readValue(data, klass);
        } catch (IOException e) {
            throw new BodyMappingException(e);
        }
    }

    public PostingInfo getPosting(String nodeName, String postingId) throws NodeApiException {
        return call("GET", nodeName, String.format("/postings/%s", Util.ue(postingId)), PostingInfo.class);
    }

    public PostingRevisionInfo getPostingRevision(String nodeName, String postingId, String revisionId)
            throws NodeApiException {

        return call("GET", nodeName,
                String.format("/postings/%s/revisions/%s", Util.ue(postingId), Util.ue(revisionId)),
                PostingRevisionInfo.class);
    }

    public PostingRevisionInfo[] getPostingRevisions(String nodeName, String postingId) throws NodeApiException {
        return call("GET", nodeName, String.format("/postings/%s/revisions", Util.ue(postingId)),
                PostingRevisionInfo[].class);
    }

    public ReactionCreated postPostingReaction(String nodeName, String postingId,
                                               ReactionDescription reactionDescription) throws NodeApiException {
        return call("POST", nodeName, String.format("/postings/%s/reactions", Util.ue(postingId)), reactionDescription,
                ReactionCreated.class);
    }

    public ReactionInfo getPostingReaction(String nodeName, String postingId, String reactionOwnerName)
            throws NodeApiException {

        return call("GET", nodeName,
                String.format("/postings/%s/reactions/%s", Util.ue(postingId), Util.ue(reactionOwnerName)),
                ReactionInfo.class);
    }

    public ReactionCreated postCommentReaction(String nodeName, String postingId, String commentId,
                                               ReactionDescription reactionDescription) throws NodeApiException {
        return call("POST", nodeName,
                String.format("/postings/%s/comments/%s/reactions", Util.ue(postingId), Util.ue(commentId)),
                reactionDescription, ReactionCreated.class);
    }

    public Result postNotification(String nodeName, NotificationPacket notificationPacket) throws NodeApiException {
        return call("POST", nodeName, "/notifications", notificationPacket, Result.class);
    }

    public SubscriberInfo postSubscriber(String nodeName, String carte, SubscriberDescriptionQ subscriber)
            throws NodeApiException {

        return call("POST", nodeName, String.format("/people/subscribers?carte=%s", Util.ue(carte)), subscriber,
                SubscriberInfo.class);
    }

    public FeedSliceInfo getFeedStories(String nodeName, String feedName, int limit) throws NodeApiException {
        return call("GET", nodeName,
                String.format("/feeds/%s/stories?limit=%d", Util.ue(feedName), limit),
                FeedSliceInfo.class);
    }

    public CommentInfo getComment(String nodeName, String postingId, String commentId) throws NodeApiException {
        return call("GET", nodeName, String.format("/postings/%s/comments/%s", Util.ue(postingId), Util.ue(commentId)),
                CommentInfo.class);
    }

    public CommentCreated postComment(String nodeName, String postingId, CommentText commentText)
            throws NodeApiException {

        return call("POST", nodeName, String.format("/postings/%s/comments", Util.ue(postingId)), commentText,
                CommentCreated.class);
    }

    public CommentInfo putComment(String nodeName, String postingId, String commentId, CommentText commentText)
            throws NodeApiException {

        return call("PUT", nodeName, String.format("/postings/%s/comments/%s", Util.ue(postingId), Util.ue(commentId)),
                commentText, CommentInfo.class);
    }

}
