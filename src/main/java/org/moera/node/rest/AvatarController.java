package org.moera.node.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.output.TeeOutputStream;
import org.bouncycastle.crypto.io.DigestOutputStream;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.config.Config;
import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.MimeUtils;
import org.moera.node.model.AvatarAttributes;
import org.moera.node.model.AvatarInfo;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.ValidationFailure;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/avatars")
public class AvatarController {

    private static Logger log = LoggerFactory.getLogger(AvatarController.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private AvatarRepository avatarRepository;

    @Inject
    private MediaOperations mediaOperations;

    @PostMapping
    @Admin
    @Transactional
    public AvatarInfo post(@Valid @RequestBody AvatarAttributes avatarAttributes) throws IOException {
        log.info("POST /avatars (mediaId = {}, clipX = {}, clipY = {}, clipSize = {}, shape = {})",
                LogUtil.format(avatarAttributes.getMediaId()), LogUtil.format(avatarAttributes.getClipX()),
                LogUtil.format(avatarAttributes.getClipY()), LogUtil.format(avatarAttributes.getClipSize()),
                LogUtil.format(avatarAttributes.getShape()));

        MediaFile mediaFile = mediaFileRepository.findById(avatarAttributes.getMediaId()).orElse(null);
        if (mediaFile == null || !mediaFile.isExposed()) {
            throw new ValidationFailure("avatarAttributes.mediaId.not-found");
        }
        if (mediaFile.getSizeX() == null || mediaFile.getSizeY() == null) {
            throw new ValidationFailure("avatar.media-unsupported");
        }
        if (avatarAttributes.getClipX() + avatarAttributes.getClipSize() > mediaFile.getSizeX()) {
            throw new ValidationFailure("avatarAttributes.clipX.out-of-range");
        }
        if (avatarAttributes.getClipY() + avatarAttributes.getClipSize() > mediaFile.getSizeY()) {
            throw new ValidationFailure("avatarAttributes.clipY.out-of-range");
        }
        var thumbnailFormat = MimeUtils.thumbnail(mediaFile.getMimeType());
        if (thumbnailFormat == null) {
            throw new ValidationFailure("avatar.media-unsupported");
        }

        var tmp = mediaOperations.tmpFile();
        Path tmpPath = tmp.getFirst();
        try {
            Path mediaPath = FileSystems.getDefault().getPath(config.getMedia().getPath(), mediaFile.getFileName());

            DigestOutputStream digestStream = new DigestOutputStream(DigestFactory.getDigest("SHA-1"));
            OutputStream out = new TeeOutputStream(tmp.getSecond(), digestStream);

            Thumbnails.of(mediaPath.toFile())
                    .rotate(avatarAttributes.getRotate())
                    .sourceRegion(
                            avatarAttributes.getClipX(), avatarAttributes.getClipY(),
                            avatarAttributes.getClipSize(), avatarAttributes.getClipSize())
                    .size(avatarAttributes.getAvatarSize(), avatarAttributes.getAvatarSize())
                    .toOutputStream(out);

            String avatarId = Util.base64urlencode(digestStream.getDigest());
            MediaFile avatarFile = mediaOperations.putInPlace(avatarId, thumbnailFormat.mimeType, tmpPath);
            avatarFile.setExposed(true);

            Avatar avatar = new Avatar();
            avatar.setId(UUID.randomUUID());
            avatar.setNodeId(requestContext.nodeId());
            avatar.setMediaFile(avatarFile);
            avatar.setShape(avatarAttributes.getShape());
            avatar = avatarRepository.save(avatar);

            return new AvatarInfo(avatar);
        } catch (IOException e) {
            throw new OperationFailure("media.storage-error");
        } finally {
            Files.deleteIfExists(tmpPath);
        }
    }

    @GetMapping
    public List<AvatarInfo> getAll() {
        log.info("GET /avatars");

        return avatarRepository.findAllByNodeId(requestContext.nodeId()).stream()
                .map(AvatarInfo::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public AvatarInfo get(@PathVariable UUID id) {
        log.info("GET /avatars/{id} (id = {})", LogUtil.format(id));

        Avatar avatar = avatarRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("avatar.not-found"));
        return new AvatarInfo(avatar);
    }

}