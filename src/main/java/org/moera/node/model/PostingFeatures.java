package org.moera.node.model;

import javax.imageio.ImageIO;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.option.Options;

public class PostingFeatures {

    private boolean post;
    private boolean subjectPresent;
    private int mediaMaxSize;
    private int imageRecommendedSize;
    private int imageRecommendedPixels;

    public PostingFeatures(Options options, AccessChecker accessChecker) {
        post = accessChecker.isPrincipal(Principal.ADMIN, Scope.ADD_POST)
                || options.getBool("posting.non-admin.allowed");
        subjectPresent = options.getBool("posting.subject.present");
        int maxSize = options.getInt("media.max-size");
        mediaMaxSize = Math.min(maxSize, options.getInt("posting.media.max-size"));
        imageRecommendedSize = Math.min(mediaMaxSize, options.getInt("posting.image.recommended-size"));
        imageRecommendedPixels = options.getInt("posting.image.recommended-pixels");
    }

    public boolean isPost() {
        return post;
    }

    public void setPost(boolean post) {
        this.post = post;
    }

    public boolean isSubjectPresent() {
        return subjectPresent;
    }

    public void setSubjectPresent(boolean subjectPresent) {
        this.subjectPresent = subjectPresent;
    }

    public SourceFormat[] getSourceFormats() {
        return SourceFormat.values();
    }

    public int getMediaMaxSize() {
        return mediaMaxSize;
    }

    public void setMediaMaxSize(int mediaMaxSize) {
        this.mediaMaxSize = mediaMaxSize;
    }

    public int getImageRecommendedSize() {
        return imageRecommendedSize;
    }

    public void setImageRecommendedSize(int imageRecommendedSize) {
        this.imageRecommendedSize = imageRecommendedSize;
    }

    public int getImageRecommendedPixels() {
        return imageRecommendedPixels;
    }

    public void setImageRecommendedPixels(int imageRecommendedPixels) {
        this.imageRecommendedPixels = imageRecommendedPixels;
    }

    public String[] getImageFormats() {
        return ImageIO.getReaderMIMETypes();
    }

}
