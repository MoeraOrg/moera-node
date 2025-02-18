package org.moera.node.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.AvatarAttributes;
import org.moera.lib.node.types.AvatarInfo;
import org.moera.lib.node.types.AvatarOrdinal;
import org.moera.lib.node.types.AvatarsOrdered;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.AvatarAddedLiberin;
import org.moera.node.liberin.model.AvatarDeletedLiberin;
import org.moera.node.liberin.model.AvatarOrderedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.media.MimeUtils;
import org.moera.node.media.ThumbnailUtil;
import org.moera.node.model.AvatarInfoUtil;
import org.moera.node.model.AvatarOrdinalUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/avatars")
public class AvatarController {

    private static final Logger log = LoggerFactory.getLogger(AvatarController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private AvatarRepository avatarRepository;

    @Inject
    private MediaOperations mediaOperations;

    @PostMapping
    @Admin(Scope.UPDATE_PROFILE)
    @Transactional
    public AvatarInfo post(@RequestBody AvatarAttributes avatarAttributes) throws IOException {
        log.info(
            "POST /avatars (mediaId = {}, clipX = {}, clipY = {}, clipSize = {}, shape = {})",
            LogUtil.format(avatarAttributes.getMediaId()), LogUtil.format(avatarAttributes.getClipX()),
            LogUtil.format(avatarAttributes.getClipY()), LogUtil.format(avatarAttributes.getClipSize()),
            LogUtil.format(avatarAttributes.getShape())
        );

        avatarAttributes.validate();

        MediaFile mediaFile = mediaFileRepository.findById(avatarAttributes.getMediaId()).orElse(null);
        ValidationUtil.assertion(mediaFile != null && mediaFile.isExposed(), "media.not-found");
        ValidationUtil.assertion(
            mediaFile.getSizeX() != null && mediaFile.getSizeY() != null,
            "avatar.media-unsupported"
        );

        rotateClipToOrientation(avatarAttributes, mediaFile);
        ValidationUtil.assertion(
            avatarAttributes.getClipX() >= 0
                && avatarAttributes.getClipX() + avatarAttributes.getClipSize() <= mediaFile.getSizeX(),
            "avatar.clip-x.out-of-range"
        );
        ValidationUtil.assertion(
            avatarAttributes.getClipY() >= 0
                && avatarAttributes.getClipY() + avatarAttributes.getClipSize() <= mediaFile.getSizeY(),
            "avatar.clip-y.out-of-range"
        );

        var thumbnailFormat = MimeUtils.thumbnail(mediaFile.getMimeType());
        ValidationUtil.assertion(thumbnailFormat != null, "avatar.media-unsupported");

        var tmp = mediaOperations.tmpFile();
        try {
            DigestingOutputStream out = new DigestingOutputStream(tmp.getOutputStream());

            ThumbnailUtil.thumbnailOf(mediaOperations.getPath(mediaFile).toFile(), mediaFile.getMimeType())
                .rotate(avatarAttributes.getRotate())
                .sourceRegion(
                        avatarAttributes.getClipX(), avatarAttributes.getClipY(),
                        avatarAttributes.getClipSize(), avatarAttributes.getClipSize())
                .size(avatarAttributes.getAvatarSize(), avatarAttributes.getAvatarSize())
                .toOutputStream(out);

            MediaFile avatarFile = mediaOperations.putInPlace(
                out.getHash(), thumbnailFormat.mimeType, tmp.getPath(), out.getDigest(), true
            );
            avatarFile = mediaFileRepository.save(avatarFile);

            Avatar avatar = new Avatar();
            avatar.setId(UUID.randomUUID());
            avatar.setNodeId(requestContext.nodeId());
            avatar.setMediaFile(avatarFile);
            avatar.setShape(avatarAttributes.getShape());
            if (avatarAttributes.getOrdinal() != null) {
                avatar.setOrdinal(avatarAttributes.getOrdinal());
            } else {
                Integer ordinal = avatarRepository.maxOrdinal(requestContext.nodeId());
                avatar.setOrdinal(ordinal != null ? ordinal + 1 : 0);
            }
            avatar = avatarRepository.save(avatar);

            AvatarInfo avatarInfo = AvatarInfoUtil.build(avatar);

            requestContext.send(new AvatarAddedLiberin(avatarInfo));

            return avatarInfo;
        } catch (IOException e) {
            throw new OperationFailure("media.storage-error");
        } finally {
            Files.deleteIfExists(tmp.getPath());
        }
    }

    private void rotateClipToOrientation(AvatarAttributes avatarAttributes, MediaFile mediaFile) {
        int clipX = avatarAttributes.getClipX();
        int clipY = avatarAttributes.getClipY();
        switch (mediaFile.getOrientation()) {
            case 1:
                break;
            case 2: // Flip X
                clipX = mediaFile.getSizeX() - avatarAttributes.getClipX() - avatarAttributes.getClipSize();
                break;
            case 3: // PI rotation
                clipX = mediaFile.getSizeX() - avatarAttributes.getClipX() - avatarAttributes.getClipSize();
                clipY = mediaFile.getSizeY() - avatarAttributes.getClipY() - avatarAttributes.getClipSize();
                break;
            case 4: // Flip Y
                clipY = mediaFile.getSizeY() - avatarAttributes.getClipY() - avatarAttributes.getClipSize();
                break;
            case 5: // -PI/2 and Flip X
                clipX = mediaFile.getSizeX() - avatarAttributes.getClipY() - avatarAttributes.getClipSize();
                clipY = mediaFile.getSizeY() - avatarAttributes.getClipX() - avatarAttributes.getClipSize();
                break;
            case 6: // -PI/2
                clipX = avatarAttributes.getClipY();
                clipY = mediaFile.getSizeY() - avatarAttributes.getClipX() - avatarAttributes.getClipSize();
                break;
            case 7: // PI/2 and Flip X and Y
                clipX = avatarAttributes.getClipY();
                clipY = avatarAttributes.getClipX();
                break;
            case 8: // PI/2
                clipX = mediaFile.getSizeX() - avatarAttributes.getClipY() - avatarAttributes.getClipSize();
                clipY = avatarAttributes.getClipX();
                break;
        }
        avatarAttributes.setClipX(clipX);
        avatarAttributes.setClipY(clipY);
    }

    @PostMapping("/reorder")
    @Admin(Scope.UPDATE_PROFILE)
    @Transactional
    public AvatarOrdinal[] reorder(@RequestBody AvatarsOrdered avatarsOrdered) {
        int size = avatarsOrdered.getIds() != null ? avatarsOrdered.getIds().size() : 0;

        log.info("POST /avatars/reorder ({} items)", size);

        AvatarOrdinal[] result = new AvatarOrdinal[size];
        if (size == 0) {
            return result;
        }

        int ordinal = 0;
        for (String id : avatarsOrdered.getIds()) {
            UUID avatarId = Util.uuid(id)
                .orElseThrow(() -> new ObjectNotFoundFailure("avatar.not-found"));
            Avatar avatar = avatarRepository.findByNodeIdAndId(requestContext.nodeId(), avatarId)
                .orElseThrow(() -> new ObjectNotFoundFailure("avatar.not-found"));
            avatar.setOrdinal(ordinal);
            result[ordinal] = AvatarOrdinalUtil.build(avatar.getId().toString(), ordinal);
            requestContext.send(new AvatarOrderedLiberin(avatar));
            ordinal++;
        }

        return result;
    }

    @GetMapping
    @NoCache
    @Transactional
    public List<AvatarInfo> getAll() {
        log.info("GET /avatars");

        return avatarRepository.findAllByNodeId(requestContext.nodeId()).stream()
                .map(AvatarInfoUtil::build)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public AvatarInfo get(@PathVariable UUID id) {
        log.info("GET /avatars/{id} (id = {})", LogUtil.format(id));

        Avatar avatar = avatarRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("avatar.not-found"));
        return AvatarInfoUtil.build(avatar);
    }

    @DeleteMapping("/{id}")
    @Admin(Scope.UPDATE_PROFILE)
    @Transactional
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /avatars/{id} (id = {})", LogUtil.format(id));

        Avatar avatar = avatarRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("avatar.not-found"));
        avatarRepository.delete(avatar);

        requestContext.send(new AvatarDeletedLiberin(avatar));

        return Result.OK;
    }

}
