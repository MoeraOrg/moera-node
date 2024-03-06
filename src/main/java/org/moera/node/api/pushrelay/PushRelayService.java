package org.moera.node.api.pushrelay;

import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcService;
import org.moera.node.model.StoryInfo;

@JsonRpcService("/moera-push-relay")
public interface PushRelayService {

    void register(
            @JsonRpcParam("clientId") String clientId,
            @JsonRpcParam("nodeName") String nodeName,
            @JsonRpcParam("lang") String lang,
            @JsonRpcParam("signedAt") long signedAt,
            @JsonRpcParam("signature") byte[] signature);

    void feedStatus(
            @JsonRpcParam("feedName") String feedName,
            @JsonRpcParam("notViewed") int notViewed,
            @JsonRpcParam("notViewedMoment") long notViewedMoment,
            @JsonRpcParam("nodeName") String nodeName,
            @JsonRpcParam("signedAt") long signedAt,
            @JsonRpcParam("signature") byte[] signature);

    void storyAdded(
            @JsonRpcParam("story") StoryInfo story,
            @JsonRpcParam("nodeName") String nodeName,
            @JsonRpcParam("signedAt") long signedAt,
            @JsonRpcParam("signature") byte[] signature);

    void storyDeleted(
            @JsonRpcParam("storyId") String storyId,
            @JsonRpcParam("nodeName") String nodeName,
            @JsonRpcParam("signedAt") long signedAt,
            @JsonRpcParam("signature") byte[] signature);

}
