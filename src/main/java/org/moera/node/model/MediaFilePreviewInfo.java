package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.MediaFilePreview;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaFilePreviewInfo {

    private int targetWidth;
    private String directPath;
    private int width;
    private int height;
    private boolean original;

    public MediaFilePreviewInfo() {
    }

    public MediaFilePreviewInfo(MediaFilePreview preview, String originalDirectPath) {
        targetWidth = preview.getWidth();
        width = preview.getMediaFile().getSizeX();
        height = preview.getMediaFile().getSizeY();
        original = preview.isOriginal();
        directPath = preview.getDirectPath(originalDirectPath);
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    public String getDirectPath() {
        return directPath;
    }

    public void setDirectPath(String directPath) {
        this.directPath = directPath;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isOriginal() {
        return original;
    }

    public void setOriginal(boolean original) {
        this.original = original;
    }

}
