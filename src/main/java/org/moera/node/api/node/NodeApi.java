package org.moera.node.api.node;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.data.MediaFile;
import org.moera.node.data.SubscriptionType;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.TemporaryFile;
import org.moera.node.media.TemporaryMediaFile;
import org.moera.node.media.ThresholdReachedException;
import org.moera.node.model.CommentCreated;
import org.moera.node.model.CommentInfo;
import org.moera.node.model.CommentRevisionInfo;
import org.moera.node.model.CommentText;
import org.moera.node.model.EntryInfo;
import org.moera.node.model.FeedInfo;
import org.moera.node.model.FeedSliceInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.PostingRevisionInfo;
import org.moera.node.model.PostingText;
import org.moera.node.model.PublicMediaFileInfo;
import org.moera.node.model.ReactionCreated;
import org.moera.node.model.ReactionDescription;
import org.moera.node.model.ReactionInfo;
import org.moera.node.model.Result;
import org.moera.node.model.SheriffOrderDetailsQ;
import org.moera.node.model.SubscriberDescriptionQ;
import org.moera.node.model.SubscriberInfo;
import org.moera.node.model.SubscriberOverride;
import org.moera.node.model.UserListItemInfo;
import org.moera.node.model.UserListSliceInfo;
import org.moera.node.model.WhoAmI;
import org.moera.node.model.body.BodyMappingException;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.api.naming.RegisteredNameDetails;
import org.moera.node.notification.NotificationPacket;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class NodeApi {

    private static final Duration CALL_API_CONNECTION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration CALL_API_REQUEST_TIMEOUT = Duration.ofMinutes(1);

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private NamingCache namingCache;

    @Inject
    private MediaOperations mediaOperations;

    private String fetchNodeUri(String remoteNodeName) {
        RegisteredNameDetails details = namingCache.get(remoteNodeName);
        return details != null ? UriUtil.normalize(details.getNodeUri()) : null;
    }

    private <T> T call(String method, String remoteNodeName, String location, String auth, Class<T> result)
            throws NodeApiException {

        return call(method, remoteNodeName, location, auth, null, result);
    }

    private <T> T call(String method, String remoteNodeName, String location, String auth, Object body, Class<T> result)
            throws NodeApiException {

        HttpRequest request = buildRequest(method, remoteNodeName, location, auth, body, null);
        HttpResponse<String> response;
        try {
            response = buildClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new NodeApiException(e);
        }
        validateResponseStatus(response.statusCode(), response.uri(), response::body);

        return jsonParse(response.body(), result);
    }

    private <T> T call(String method, String remoteNodeName, String location, String auth, String mimeType, Path body,
                       Class<T> result) throws NodeApiException {
        HttpRequest request = buildRequest(method, remoteNodeName, location, auth, body, mimeType);
        HttpResponse<String> response;
        try {
            response = buildClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new NodeApiException(e);
        }
        validateResponseStatus(response.statusCode(), response.uri(), response::body);

        return jsonParse(response.body(), result);
    }

    private TemporaryMediaFile call(String method, String remoteNodeName, String location, String auth,
                                    TemporaryFile tmpFile, int maxSize) throws NodeApiException {
        HttpRequest request = buildRequest(method, remoteNodeName, location, auth, null, null);
        HttpResponse<InputStream> response;
        try {
            response = buildClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new NodeApiException(e);
        }
        validateResponseStatus(response.statusCode(), response.uri(), () -> streamToString(response.body()));

        String contentType = response.headers().firstValue("Content-Type").orElse(null);
        if (contentType == null) {
            throw new NodeApiException("Response has no Content-Type");
        }
        OptionalLong len = response.headers().firstValueAsLong("Content-Length");
        Long contentLength = len.isPresent() ? len.getAsLong() : null;
        try {
            DigestingOutputStream out = MediaOperations.transfer(
                    response.body(), tmpFile.getOutputStream(), contentLength, maxSize);
            return new TemporaryMediaFile(out.getHash(), contentType, out.getDigest());
        } catch (ThresholdReachedException e) {
            throw new NodeApiException(
                    String.format("Media %s at %s reports a wrong size or larger than %d bytes",
                            location, remoteNodeName, maxSize));
        } catch (IOException e) {
            throw new NodeApiException(
                    String.format("Error downloading media %s: %s", location, e.getMessage()));
        }
    }

    private HttpClient buildClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CALL_API_CONNECTION_TIMEOUT)
                .build();
    }

    private HttpRequest buildRequest(String method, String remoteNodeName, String location, String auth,
                                     Object body, String mimeType) throws NodeApiException {
        String nodeUri = fetchNodeUri(remoteNodeName);
        if (nodeUri == null) {
            throw new NodeApiUnknownNameException(remoteNodeName);
        }

        var requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(nodeUri + "/api" + location))
                .timeout(CALL_API_REQUEST_TIMEOUT);
        if (body instanceof Path) {
            try {
                requestBuilder = requestBuilder
                        .header(HttpHeaders.CONTENT_TYPE, mimeType)
                        .method(method, HttpRequest.BodyPublishers.ofFile((Path) body));
            } catch (FileNotFoundException e) {
                throw new NodeApiException(
                        String.format("Cannot send a file %s", Objects.toString(body, "null")), e);
            }
        } else {
            requestBuilder = requestBuilder
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .method(method, jsonPublisher(body));
        }
        if (auth != null) {
            requestBuilder = requestBuilder.header(HttpHeaders.AUTHORIZATION, "bearer " + auth);
        }

        return requestBuilder.build();
    }

    private void validateResponseStatus(int status, URI uri, Supplier<String> bodySupplier)
            throws NodeApiErrorStatusException {

        String body = null;
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        switch (httpStatus) {
            case NOT_FOUND:
                throw new NodeApiNotFoundException(uri);

            case FORBIDDEN:
                throw new NodeApiAuthenticationException();

            case OK:
            case CREATED:
                // do nothing
                break;

            case BAD_REQUEST:
            case CONFLICT:
                body = bodySupplier.get();
                try {
                    Result answer = jsonParse(body, Result.class);
                    switch (httpStatus) {
                        case BAD_REQUEST:
                            throw new NodeApiValidationException(answer.getErrorCode());
                        case CONFLICT:
                            throw new NodeApiOperationException(answer.getErrorCode());
                    }
                } catch (BodyMappingException e) {
                    // fallthru
                }
                // fallthru

            default:
                if (body == null) {
                    body = bodySupplier.get();
                }
                throw new NodeApiErrorStatusException(status, body);
        }
    }

    private String auth(String type, String token) {
        if (token == null) {
            return null;
        }
        return type + ":" + token;
    }

    private String streamToString(InputStream in) {
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
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

    public WhoAmI whoAmI(String nodeName) throws NodeApiException {
        return call("GET", nodeName, "/whoami", null, WhoAmI.class);
    }

    public PostingInfo getPosting(String nodeName, String carte, String postingId) throws NodeApiException {
        return call("GET", nodeName, String.format("/postings/%s", Util.ue(postingId)),
                auth("carte", carte), PostingInfo.class);
    }

    public PostingRevisionInfo getPostingRevision(String nodeName, String carte, String postingId, String revisionId)
            throws NodeApiException {

        return call("GET", nodeName,
                String.format("/postings/%s/revisions/%s", Util.ue(postingId), Util.ue(revisionId)),
                auth("carte", carte), PostingRevisionInfo.class);
    }

    public PostingInfo postPosting(String nodeName, PostingText postingText) throws NodeApiException {
        return call("POST", nodeName, "/postings", null, postingText, PostingInfo.class);
    }

    public PostingInfo putPosting(String nodeName, String postingId, PostingText postingText) throws NodeApiException {
        return call("PUT", nodeName, String.format("/postings/%s", Util.ue(postingId)), null, postingText,
                PostingInfo.class);
    }

    public ReactionCreated postPostingReaction(String nodeName, String postingId,
                                               ReactionDescription reactionDescription) throws NodeApiException {
        return call("POST", nodeName, String.format("/postings/%s/reactions", Util.ue(postingId)), null,
                reactionDescription, ReactionCreated.class);
    }

    public ReactionInfo getPostingReaction(String nodeName, String carte, String postingId, String reactionOwnerName)
            throws NodeApiException {

        return call("GET", nodeName,
                String.format("/postings/%s/reactions/%s", Util.ue(postingId), Util.ue(reactionOwnerName)),
                auth("carte", carte), ReactionInfo.class);
    }

    public ReactionCreated postCommentReaction(String nodeName, String postingId, String commentId,
                                               ReactionDescription reactionDescription) throws NodeApiException {
        return call("POST", nodeName,
                String.format("/postings/%s/comments/%s/reactions", Util.ue(postingId), Util.ue(commentId)), null,
                reactionDescription, ReactionCreated.class);
    }

    public Result postNotification(String nodeName, NotificationPacket notificationPacket) throws NodeApiException {
        return call("POST", nodeName, "/notifications", null, notificationPacket, Result.class);
    }

    public SubscriberInfo postSubscriber(String nodeName, String carte, SubscriberDescriptionQ subscriber)
            throws NodeApiException {

        return call("POST", nodeName, "/people/subscribers", auth("carte", carte), subscriber,
                SubscriberInfo.class);
    }

    public Result deleteSubscriber(String nodeName, String carte, String subscriberId) throws NodeApiException {
        return call("DELETE", nodeName, String.format("/people/subscribers/%s", Util.ue(subscriberId)),
                auth("carte", carte), Result.class);
    }

    public FeedInfo getFeed(String nodeName, String feedName) throws NodeApiException {
        return call("GET", nodeName, String.format("/feeds/%s", Util.ue(feedName)), null, FeedInfo.class);
    }

    public FeedSliceInfo getFeedStories(String nodeName, String feedName, int limit) throws NodeApiException {
        return call("GET", nodeName,
                String.format("/feeds/%s/stories?limit=%d", Util.ue(feedName), limit), null,
                FeedSliceInfo.class);
    }

    public CommentInfo getComment(String nodeName, String carte, String postingId,
                                  String commentId) throws NodeApiException {
        return call("GET", nodeName,
                String.format("/postings/%s/comments/%s", Util.ue(postingId), Util.ue(commentId)),
                auth("carte", carte), CommentInfo.class);
    }

    public CommentRevisionInfo getCommentRevision(String nodeName, String carte, String postingId, String commentId,
                                                  String revisionId) throws NodeApiException {
        return call("GET", nodeName,
                String.format("/postings/%s/comments/%s/revisions/%s",
                        Util.ue(postingId), Util.ue(commentId), Util.ue(revisionId)), auth("carte", carte),
                CommentRevisionInfo.class);
    }

    public CommentCreated postComment(String nodeName, String postingId, CommentText commentText)
            throws NodeApiException {

        return call("POST", nodeName, String.format("/postings/%s/comments", Util.ue(postingId)), null,
                commentText, CommentCreated.class);
    }

    public CommentInfo putComment(String nodeName, String postingId, String commentId, CommentText commentText)
            throws NodeApiException {

        return call("PUT", nodeName,
                String.format("/postings/%s/comments/%s", Util.ue(postingId), Util.ue(commentId)), null,
                commentText, CommentInfo.class);
    }

    public ReactionInfo getCommentReaction(String nodeName, String carte, String postingId, String commentId,
                                           String reactionOwnerName) throws NodeApiException {
        return call("GET", nodeName,
                String.format("/postings/%s/comments/%s/reactions/%s",
                        Util.ue(postingId), Util.ue(commentId), Util.ue(reactionOwnerName)), auth("carte", carte),
                ReactionInfo.class);
    }

    public TemporaryMediaFile getPublicMedia(String nodeName, String id, TemporaryFile tmpFile, int maxSize)
            throws NodeApiException {

        return call("GET", nodeName, String.format("/media/public/%s/data", Util.ue(id)), null,
                tmpFile, maxSize);
    }

    public PublicMediaFileInfo getPublicMediaInfo(String nodeName, String id) throws NodeApiException {
        try {
            return call("GET", nodeName, String.format("/media/public/%s/info", Util.ue(id)), null,
                    PublicMediaFileInfo.class);
        } catch (NodeApiNotFoundException e) {
            return null;
        }
    }

    public PublicMediaFileInfo postPublicMedia(String nodeName, String carte,
                                               MediaFile mediaFile) throws NodeApiException {
        return call("POST", nodeName, "/media/public", auth("carte", carte),
                mediaFile.getMimeType(), mediaOperations.getPath(mediaFile), PublicMediaFileInfo.class);
    }

    public TemporaryMediaFile getPrivateMedia(String nodeName, String carte, String id, TemporaryFile tmpFile,
                                              int maxSize) throws NodeApiException {
        return call("GET", nodeName, String.format("/media/private/%s/data", Util.ue(id)),
                auth("carte", carte), tmpFile, maxSize);
    }

    public EntryInfo[] getPrivateMediaParent(String nodeName, String carte, String id) throws NodeApiException {
        try {
            return call("GET", nodeName, String.format("/media/private/%s/parent", Util.ue(id)),
                    auth("carte", carte), EntryInfo[].class);
        } catch (NodeApiNotFoundException e) {
            return null;
        }
    }

    public SubscriberInfo[] getSubscribers(String nodeName, String carte, String remoteNodeName, SubscriptionType type,
                                           String feedName, String entryId) throws NodeApiException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("/people/subscribers");
        if (remoteNodeName != null) {
            builder = builder.queryParam("nodeName", remoteNodeName);
        }
        if (type != null) {
            builder = builder.queryParam("type", type.getValue());
        }
        if (feedName != null) {
            builder = builder.queryParam("feedName", feedName);
        }
        if (entryId != null) {
            builder = builder.queryParam("entryId", entryId);
        }
        return call("GET", nodeName, builder.build().toUriString(), auth("carte", carte),
                SubscriberInfo[].class);
    }

    public SubscriberInfo putSubscriber(String nodeName, String carte, String id,
                                        SubscriberOverride subscriberOverride) throws NodeApiException {
        return call("PUT", nodeName, String.format("/people/subscribers/%s", Util.ue(id)),
                auth("carte", carte), subscriberOverride, SubscriberInfo.class);
    }

    public Result postSheriffOrder(String nodeName, SheriffOrderDetailsQ sheriffOrderDetails) throws NodeApiException {
        return call("POST", nodeName, "/sheriff/orders", null, sheriffOrderDetails, Result.class);
    }

    public UserListSliceInfo getUserListItems(String nodeName, String listName, long before) throws NodeApiException {
        return call("GET", nodeName, String.format("/user-lists/%s/items?before=%d", Util.ue(listName), before),
                null, UserListSliceInfo.class);
    }

    public UserListItemInfo getUserListItem(String nodeName, String listName, String name) throws NodeApiException {
        return call("GET", nodeName, String.format("/user-lists/%s/items/%s", Util.ue(listName), Util.ue(name)),
                null, UserListItemInfo.class);
    }

}
