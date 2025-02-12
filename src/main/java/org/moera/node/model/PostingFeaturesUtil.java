package org.moera.node.model;

import java.util.Arrays;
import javax.imageio.ImageIO;

import org.moera.lib.node.types.PostingFeatures;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.principal.AccessChecker;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.option.Options;

public class PostingFeaturesUtil {

    public static PostingFeatures build(Options options, AccessChecker accessChecker) {
        PostingFeatures postingFeatures = new PostingFeatures();
        postingFeatures.setPost(
            accessChecker.isPrincipal(Principal.ADMIN, Scope.ADD_POST)
            || options.getBool("posting.non-admin.allowed")
        );
        postingFeatures.setSubjectPresent(options.getBool("posting.subject.present"));
        postingFeatures.setSourceFormats(Arrays.asList(SourceFormat.values()));
        postingFeatures.setMediaMaxSize(
            Math.min(options.getInt("media.max-size"), options.getInt("posting.media.max-size"))
        );
        postingFeatures.setImageRecommendedSize(
            Math.min(postingFeatures.getMediaMaxSize(), options.getInt("posting.image.recommended-size"))
        );
        postingFeatures.setImageRecommendedPixels(options.getInt("posting.image.recommended-pixels"));
        postingFeatures.setImageFormats(Arrays.asList(ImageIO.getReaderMIMETypes()));
        return postingFeatures;
    }

}
