package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.node.liberin.Liberin;

public class RemoteMediaDownloadedLiberin extends Liberin {

    private String nodeName;
    private String mediaId;
    private PrivateMediaFileInfo mediaInfo;

    public RemoteMediaDownloadedLiberin(String nodeName, String mediaId, PrivateMediaFileInfo mediaInfo) {
        this.nodeName = nodeName;
        this.mediaId = mediaId;
        this.mediaInfo = mediaInfo;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public PrivateMediaFileInfo getMediaInfo() {
        return mediaInfo;
    }

    public void setMediaInfo(PrivateMediaFileInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("mediaId", mediaId);
        model.put("media", mediaInfo);
    }

}
