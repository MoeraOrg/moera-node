package org.moera.node.ui;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.github.jknack.handlebars.Handlebars.SafeString;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthCategory;
import org.moera.node.auth.AuthenticationCategory;
import org.moera.node.auth.principal.Principal;
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
import org.moera.node.operations.FeedOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(MediaUiController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private FeedOperations feedOperations;

    @GetMapping("/public/{id}.{ext}")
    @MaxCache
    @Transactional
    @ResponseBody
    public ResponseEntity<Resource> getDataPublic(@PathVariable String id,
                                                  @RequestParam(required = false) Integer width,
                                                  @RequestParam(required = false) Boolean download) {
        log.info("GET MEDIA /media/public/{id}.ext (id = {})", LogUtil.format(id));

        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null || !mediaFile.isExposed()) {
            throw new PageNotFoundException();
        }
        return mediaOperations.serve(mediaFile, width, download);
    }

    private boolean includesAdmin(Principal viewPrincipal) {
        return viewPrincipal.includes(true, requestContext.nodeName(), false, new String[0]);
    }

    @GetMapping("/private/{id}.{ext}")
    @AuthenticationCategory(AuthCategory.VIEW_MEDIA)
    @MaxCache
    @Transactional
    @ResponseBody
    public ResponseEntity<Resource> getDataPrivate(@PathVariable UUID id,
                                                   @RequestParam(required = false) Integer width,
                                                   @RequestParam(required = false) Boolean download) {
        log.info("GET MEDIA /media/private/{id}.ext (id = {})", LogUtil.format(id));

        MediaFileOwner mediaFileOwner =  mediaFileOwnerRepository.findFullById(requestContext.nodeId(), id)
                .orElseThrow(PageNotFoundException::new);
        Principal viewPrincipal = mediaFileOwner.getViewE(requestContext.nodeName());
        if (!requestContext.isPrincipal(viewPrincipal)
                && !feedOperations.isSheriffAllowed(() -> mediaOperations.getParentStories(id), viewPrincipal)
                && !(includesAdmin(viewPrincipal) && requestContext.isClient(requestContext.nodeName()))) {
                // The exception above is made to allow authentication with a carte as admin to view admin-only
                // media instead of passing admin tokens in parameters
            throw new PageNotFoundException();
        }
        return mediaOperations.serve(mediaFileOwner.getMediaFile(), width, download);
    }

    @GetMapping(path = "/private/{id}/caption", produces = "text/html")
    @Transactional
    public String getCaptionPrivate(@PathVariable UUID id, Model model) {
        log.info("GET MEDIA /media/private/{id}/caption (id = {})", LogUtil.format(id));

        MediaFileOwner mediaFileOwner =  mediaFileOwnerRepository.findFullById(requestContext.nodeId(), id)
                .orElseThrow(PageNotFoundException::new);
        Posting posting = mediaFileOwner.getPosting(null);
        if (posting == null) {
            throw new PageNotFoundException();
        }
        if (!requestContext.isPrincipal(posting.getViewE())
                && !feedOperations.isSheriffAllowed(() -> mediaOperations.getParentStories(id), posting.getViewE())
                && !(includesAdmin(posting.getViewE()) && requestContext.isClient(requestContext.nodeName()))) {
                // See the comment above
            throw new PageNotFoundException();
        }
        String body = posting.getCurrentRevision().getSaneBody();
        body = body != null ? body : "";
        model.addAttribute("posting", new PostingInfo(posting, requestContext));
        model.addAttribute("caption", new SafeString(body));

        return "caption";
    }

}
