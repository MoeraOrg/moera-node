package org.moera.node.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.moera.commons.util.LogUtil;
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
import org.moera.node.model.AvatarAttributes;
import org.moera.node.model.AvatarInfo;
import org.moera.node.model.AvatarOrdinal;
import org.moera.node.model.AvatarsOrdered;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.util.DigestingOutputStream;
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

        rotateClipToOrientation(avatarAttributes, mediaFile);
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
                    out.getHash(), thumbnailFormat.mimeType, tmp.getPath(), out.getDigest());
            avatarFile.setExposed(true);
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

            AvatarInfo avatarInfo = new AvatarInfo(avatar);

            requestContext.send(new AvatarAddedLiberin(avatarInfo));

            return avatarInfo;
        } catch (IOException e) {
            throw new OperationFailure("media.storage-error");
        } finally {
            Files.deleteIfExists(tmp.getPath());
        }
    }

    private int getImageOrientation(Path imagePath) {
        int orientation = 1;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imagePath.toFile());
            Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (MetadataException | IOException | ImageProcessingException e) {
            // Could not get orientation, use default
        }
        return orientation;
    }

    private void rotateClipToOrientation(AvatarAttributes avatarAttributes, MediaFile mediaFile) {
        int orientation = getImageOrientation(mediaOperations.getPath(mediaFile));
        int clipX = avatarAttributes.getClipX();
        int clipY = avatarAttributes.getClipY();
        switch (orientation) {
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
    @Admin
    @Transactional
    public AvatarOrdinal[] reorder(@Valid @RequestBody AvatarsOrdered avatarsOrdered) {
        int size = avatarsOrdered.getIds() != null ? avatarsOrdered.getIds().length : 0;

        log.info("POST /avatars/reorder ({} items)", size);

        AvatarOrdinal[] result = new AvatarOrdinal[size];
        if (size == 0) {
            return result;
        }

        int ordinal = 0;
        for (UUID id : avatarsOrdered.getIds()) {
            Avatar avatar = avatarRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                    .orElseThrow(() -> new ObjectNotFoundFailure("avatar.not-found"));
            avatar.setOrdinal(ordinal);
            result[ordinal] = new AvatarOrdinal(avatar.getId().toString(), ordinal);
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
                .map(AvatarInfo::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Transactional
    public AvatarInfo get(@PathVariable UUID id) {
        log.info("GET /avatars/{id} (id = {})", LogUtil.format(id));

        Avatar avatar = avatarRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("avatar.not-found"));
        return new AvatarInfo(avatar);
    }

    @DeleteMapping("/{id}")
    @Admin
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
