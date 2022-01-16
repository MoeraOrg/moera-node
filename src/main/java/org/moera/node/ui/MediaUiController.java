package org.moera.node.ui;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.github.jknack.handlebars.Handlebars.SafeString;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.Posting;
import org.moera.node.global.MaxCache;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.PostingInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@UiController
@RequestMapping("/moera/media")
public class MediaUiController {

    @Inject
    private RequestContext requestContext;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private MediaOperations mediaOperations;

    @GetMapping("/public/{id}.{ext}")
    @MaxCache
    @Transactional
    @ResponseBody
    public ResponseEntity<Resource> getDataPublic(@PathVariable String id,
                                                  @RequestParam(required = false) Integer width) {
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null || !mediaFile.isExposed()) {
            throw new PageNotFoundException();
        }
        return mediaOperations.serve(mediaFile, width);
    }

    @GetMapping("/private/{id}.{ext}")
    @MaxCache
    @Transactional
    @ResponseBody
    public ResponseEntity<Resource> getDataPrivate(@PathVariable UUID id,
                                                   @RequestParam(required = false) Integer width) {
        MediaFileOwner mediaFileOwner =  mediaFileOwnerRepository.findFullById(requestContext.nodeId(), id)
                .orElseThrow(PageNotFoundException::new);
        return mediaOperations.serve(mediaFileOwner.getMediaFile(), width);
    }

    @GetMapping("/private/{id}/caption")
    @Transactional
    public String getCaptionPrivate(@PathVariable UUID id, Model model) {
        MediaFileOwner mediaFileOwner =  mediaFileOwnerRepository.findFullById(requestContext.nodeId(), id)
                .orElseThrow(PageNotFoundException::new);
        Posting posting = mediaFileOwner.getPosting(null);
        String body = posting != null ? posting.getCurrentRevision().getSaneBody() : "";
        model.addAttribute("posting", new PostingInfo(posting, false));
        model.addAttribute("caption", new SafeString(body));

        return "caption";
    }

}
