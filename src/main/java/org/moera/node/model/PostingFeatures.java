package org.moera.node.model;

import java.util.List;
import javax.imageio.ImageIO;

import org.moera.node.data.SourceFormat;
import org.moera.node.option.Options;

public class PostingFeatures {

    private boolean subjectPresent;
    private final List<Choice> sourceFormats = Choice.forEnum(SourceFormat.class);
    private int mediaMaxSize;
    private int imageRecommendedSize;
    private int imageRecommendedPixels;

    public PostingFeatures(Options options) {
        subjectPresent = options.getBool("posting.subject.present");
        int maxSize = options.getInt("media.max-size");
        mediaMaxSize = Math.min(maxSize, options.getInt("posting.media.max-size"));
        imageRecommendedSize = Math.min(mediaMaxSize, options.getInt("posting.image.recommended-size"));
        imageRecommendedPixels = options.getInt("posting.image.recommended-pixels");
    }

    public boolean isSubjectPresent() {
        return subjectPresent;
    }

    public void setSubjectPresent(boolean subjectPresent) {
        this.subjectPresent = subjectPresent;
    }

    public List<Choice> getSourceFormats() {
        return sourceFormats;
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
