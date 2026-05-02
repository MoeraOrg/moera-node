package org.moera.node.ui;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.github.jknack.handlebars.Handlebars.SafeString;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.global.MaxCache;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.media.MediaGrantProperties;
import org.moera.node.media.MediaGrantValidator;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.operations.FeedOperations;
import org.moera.node.operations.MediaAttachmentsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
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
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private FeedOperations feedOperations;

    @Inject
    private MediaGrantValidator mediaGrantValidator;

    @GetMapping("/public/{id}.{ext}")
    @MaxCache
    @Transactional
    @ResponseBody
    public ResponseEntity<Resource> getDataPublic(
        @PathVariable String id,
        @RequestParam(required = false) Integer width,
        @RequestParam(required = false) Boolean download
    ) {
        log.info("GET MEDIA /media/public/{id}.ext (id = {})", LogUtil.format(id));

        if (id.endsWith("=")) { // backward compatibility
            id = id.substring(0, id.length() - 1);
        }
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null || !mediaFile.isExposed()) {
            throw new PageNotFoundException();
        }

        return mediaOperations.serve(mediaFile, width, null, download);
    }

    private boolean includesAdmin(Principal viewPrincipal) {
        return viewPrincipal.includes(true, requestContext.nodeName(), false, new String[0]);
    }

    @GetMapping("/private/{id}.{ext}")
    @MaxCache
    @Transactional
    @ResponseBody
    public ResponseEntity<Resource> getDataPrivate(
        @PathVariable UUID id,
        @RequestParam(required = false) Integer width,
        @RequestParam(required = false) Boolean download,
        @RequestParam(name = "grant", required = false) String grantS,
        @RequestParam(name = "ignoremalware", required = false) Boolean ignoreMalware
    ) {
        log.info("GET MEDIA /media/private/{id}.ext (id = {})", LogUtil.format(id));

        MediaFileOwner mediaFileOwner =  mediaFileOwnerRepository.findFullById(requestContext.nodeId(), id)
            .orElseThrow(PageNotFoundException::new);
        MediaGrantProperties grant = mediaGrantValidator.validate(grantS, id);
        if (
            grant == null
            && !mediaFileOwner.isUnrestricted()
            && !requestContext.isAdmin(Scope.VIEW_MEDIA)
            && !requestContext.isClient(requestContext.nodeName(), Scope.VIEW_MEDIA)
        ) {
            throw new PageNotFoundException();
        }
        mediaOperations.blockMalware(mediaFileOwner, ignoreMalware);

        String title = mediaFileOwner.getTitle();
        if (grant != null) {
            if (grant.isDownload()) {
                download = true;
            }
            if (!ObjectUtils.isEmpty(grant.getFileName())) {
                title = grant.getFileName();
            }
        }

        return mediaOperations.serve(mediaFileOwner.getMediaFile(), width, title, download);
    }

    @GetMapping(path = "/private/caption/{postingId}", produces = "text/html")
    @Transactional
    public String getCaptionPrivate(@PathVariable UUID postingId, Model model) {
        log.info(
            "GET MEDIA /media/private/caption/{postingId} (postingId = {})",
            LogUtil.format(postingId)
        );

        Posting posting = postingRepository.findFullByNodeIdAndId(requestContext.nodeId(), postingId)
            .orElseThrow(PageNotFoundException::new);
        if (
            !requestContext.isPrincipal(posting.getViewE(), Scope.VIEW_CONTENT)
            && !feedOperations.isSheriffAllowed(
                () -> mediaOperations.getParentStories(posting.getParentMedia().getId()),
                posting.getViewE()
            )
            && !(
                includesAdmin(posting.getViewE())
                && requestContext.isClient(requestContext.nodeName(), Scope.VIEW_CONTENT)
            )
            // See the comment above
        ) {
            throw new PageNotFoundException();
        }
        String body = posting.getCurrentRevision().getSaneBody();
        body = body != null ? body : "";
        model.addAttribute(
            "posting",
            PostingInfoUtil.build(
                posting,
                MediaAttachmentsProvider.NONE,
                requestContext,
                requestContext.getOptions(),
                config.getMedia().getDirectServe()
            )
        );
        model.addAttribute("caption", new SafeString(body));

        return "caption";
    }

}
