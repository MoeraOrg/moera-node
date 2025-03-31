package org.moera.node.media;

public record TemporaryMediaFile(String mediaFileId, String contentType, byte[] digest) {
}
