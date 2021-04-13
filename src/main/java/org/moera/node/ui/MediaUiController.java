package org.moera.node.ui;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UiController;
import org.moera.node.media.MediaOperations;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @ResponseBody
    public ResponseEntity<Resource> getDataPublic(@PathVariable String id) {
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null || !mediaFile.isExposed()) {
            throw new PageNotFoundException();
        }
        return mediaOperations.serve(mediaFile);
    }

    @GetMapping("/private/{id}.{ext}")
    public ResponseEntity<Resource> getDataPrivate(@PathVariable UUID id) {
        MediaFileOwner mediaFileOwner =  mediaFileOwnerRepository.findFullById(requestContext.nodeId(), id)
                .orElseThrow(PageNotFoundException::new);
        return mediaOperations.serve(mediaFileOwner.getMediaFile());
    }

}
