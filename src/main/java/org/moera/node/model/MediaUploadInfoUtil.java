package org.moera.node.model;

import java.util.Arrays;

import org.moera.lib.node.types.MediaUploadInfo;
import org.moera.node.data.MediaUpload;
import org.moera.node.util.Util;

public class MediaUploadInfoUtil {

    public static MediaUploadInfo build(MediaUpload mediaUpload) {
        MediaUploadInfo info = new MediaUploadInfo();

        info.setId(mediaUpload.getId().toString());
        info.setMimeType(mediaUpload.getMimeType());
        info.setTitle(mediaUpload.getTitle());
        info.setFileSize(mediaUpload.getFileSize());
        info.setChunkSize(mediaUpload.getChunkSize());
        info.setUploadedChunks(Arrays.asList(mediaUpload.getUploadedChunks()));
        info.setDeadline(Util.toEpochSecond(mediaUpload.getDeadline()));
        info.setCompletedAt(Util.toEpochSecond(mediaUpload.getCompletedAt()));

        return info;
    }

}
