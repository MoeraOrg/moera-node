package org.moera.node.model;

import java.util.List;
import javax.imageio.ImageIO;

import org.moera.node.data.SourceFormat;
import org.moera.node.option.Options;

public class PostingFeatures {

    private boolean subjectPresent;
    private final List<Choice> sourceFormats = Choice.forEnum(SourceFormat.class);
    private long mediaMaxSize;
    private long imageRecommendedSize;
    private int imageRecommendedPixels;

    public PostingFeatures() {
    }

    public PostingFeatures(Options options) {
        subjectPresent = options.getBool("posting.subject.present");
        mediaMaxSize = options.getLong("posting.media.max-size");
        imageRecommendedSize = options.getLong("posting.image.recommended-size");
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

    public long getMediaMaxSize() {
        return mediaMaxSize;
    }

    public long getImageRecommendedSize() {
        return imageRecommendedSize;
    }

    public void setImageRecommendedSize(long imageRecommendedSize) {
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
