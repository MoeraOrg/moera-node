package org.moera.node.media;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.tika.Tika;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.AvatarDescription;
import org.moera.lib.node.types.PostingFeatures;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.config.Config;
import org.moera.node.data.ChildOperationsUtil;
import org.moera.node.data.Comment;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.data.MediaFilePreviewRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.CommentMediaTextUpdatedLiberin;
import org.moera.node.liberin.model.DraftUpdatedLiberin;
import org.moera.node.liberin.model.PostingMediaTextUpdatedLiberin;
import org.moera.node.model.AvatarDescriptionUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.PostingFeaturesUtil;
import org.moera.node.operations.OcrJob;
import org.moera.node.operations.PostingOperations;
import org.moera.node.task.Jobs;
import org.moera.node.userlist.MalwareListOperations;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class MediaOperations {

    public static final String TMP_DIR = "tmp";

    private static final Logger log = LoggerFactory.getLogger(MediaOperations.class);

    private static final int[] PREVIEW_SIZES = {1400, 900, 150};

    private static final int PERMISSIONS_REFRESH_BATCH_SIZE = 100;
    private static final int PERMISSIONS_REFRESH_BATCHES_PER_CALL = 5;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Config config;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private MediaFilePreviewRepository mediaFilePreviewRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private DraftRepository draftRepository;

    @Inject
    private MalwareListOperations malwareListOperations;

    @Inject
    @Lazy
    private PostingOperations postingOperations;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

    public TemporaryFile tmpFile() {
        while (true) {
            Path path;
            do {
                path = FileSystems.getDefault().getPath(
                    config.getMedia().getPath(), TMP_DIR, CryptoUtil.token().substring(0, 16)
                );
            } while (Files.exists(path));
            try {
                return new TemporaryFile(path, Files.newOutputStream(path, CREATE));
            } catch (IOException e) {
                // next try
            }
        }
    }

    public Path getPath(MediaFile mediaFile) {
        return FileSystems.getDefault().getPath(config.getMedia().getPath(), mediaFile.getFileName());
    }

    public static DigestingOutputStream transfer(
        InputStream in, OutputStream out, Long contentLength, int maxSize
    ) throws IOException {
        DigestingOutputStream digestingStream = new DigestingOutputStream(out);

        out = digestingStream;
        if (maxSize > 0) {
            if (contentLength != null) {
                if (contentLength > maxSize) {
                    throw new ThresholdReachedException();
                }
                in = BoundedInputStream.builder().setInputStream(in).setMaxCount(contentLength).get();
            } else {
                out = new BoundedOutputStream(out, maxSize);
            }
        }

        try {
            in.transferTo(out);
        } finally {
            out.close();
        }

        return digestingStream;
    }

    private static Dimension getImageDimension(String contentType, Path path) throws InvalidImageException {
        Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(contentType);
        while (it.hasNext()) {
            ImageReader reader = it.next();
            try {
                ImageInputStream stream = new FileImageInputStream(path.toFile());
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                return new Dimension(width, height);
            } catch (IOException e) {
                log.warn("Error reading image file {} (Content-Type: {}): {}",
                        LogUtil.format(path.toString()), LogUtil.format(contentType), e.getMessage());
            } finally {
                reader.dispose();
            }
        }

        throw new InvalidImageException();
    }

    @Transactional(REQUIRES_NEW)
    public MediaFile putInPlace(
        String id, String contentType, Path tmpPath, byte[] digest, boolean exposed
    ) throws IOException {
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null) {
            if (digest == null) {
                digest = digest(tmpPath);
            }
            // image type may be wrong, autodetection will give more accurate result
            if (contentType == null || contentType.startsWith("image/")) {
                contentType = detectContentType(tmpPath, contentType);
            }
            if (contentType == null) {
                contentType = new Tika().detect(tmpPath);
            }
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            if (exposed && !MimeUtil.isSupportedImage(contentType)) {
                throw new InvalidImageException();
            }

            Path mediaPath = FileSystems.getDefault().getPath(
                config.getMedia().getPath(), MimeUtil.fileName(id, contentType)
            );
            Files.move(tmpPath, mediaPath, REPLACE_EXISTING);

            mediaFile = new MediaFile();
            mediaFile.setId(id);
            mediaFile.setMimeType(contentType);
            if (MimeUtil.isSupportedImage(contentType)) {
                mediaFile.setDimension(getImageDimension(contentType, mediaPath));
                mediaFile.setOrientation(getImageOrientation(mediaPath));
            }
            mediaFile.setFileSize(Files.size(mediaPath));
            mediaFile.setDigest(digest);
            mediaFile.setExposed(exposed);
            mediaFile = mediaFileRepository.save(mediaFile);
        } else if (exposed && !mediaFile.isExposed()) {
            mediaFile.setExposed(exposed);
        }
        return mediaFile;
    }

    private short getImageOrientation(Path imagePath) {
        short orientation = 1;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imagePath.toFile());
            if (metadata != null) {
                Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (directory != null) {
                    orientation = (short) directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                }
            }
        } catch (MetadataException | IOException | ImageProcessingException e) {
            // Could not get orientation, use default
        }
        return orientation;
    }

    private String detectContentType(Path path, String defaultContentType) throws IOException {
        FileType fileType = FileTypeDetector.detectFileType(new BufferedInputStream(new FileInputStream(path.toFile())));
        return fileType != null ? fileType.getMimeType() : defaultContentType;
    }

    public byte[] digest(MediaFile mediaFile) throws IOException {
        return digest(
            FileSystems.getDefault().getPath(
                config.getMedia().getPath(), MimeUtil.fileName(mediaFile.getId(), mediaFile.getMimeType())
            )
        );
    }

    private static byte[] digest(Path mediaPath) throws IOException {
        DigestingOutputStream out = new DigestingOutputStream(OutputStream.nullOutputStream());
        try (InputStream in = new FileInputStream(mediaPath.toFile())) {
            in.transferTo(out);
        }
        return out.getDigest();
    }

    private static Rectangle getPreviewRegion(MediaFile mediaFile) {
        int width = mediaFile.getSizeX();
        int height = mediaFile.getSizeY();
        if (mediaFile.getSizeX() > mediaFile.getSizeY() * 5) {
            width = mediaFile.getSizeY() * 5;
        } else if (mediaFile.getSizeY() > mediaFile.getSizeX() * 5) {
            height = mediaFile.getSizeX() * 5;
        }
        return new Rectangle(width, height);
    }

    private MediaFile cropOriginal(MediaFile original) throws IOException {
        var previewFormat = MimeUtil.thumbnail(original.getMimeType());
        if (previewFormat == null) {
            return original;
        }

        Rectangle region = getPreviewRegion(original);
        if (region.getSize().equals(original.getDimension())) {
            return original;
        }

        var tmp = tmpFile();
        try {
            DigestingOutputStream out = new DigestingOutputStream(tmp.outputStream());

            ThumbnailUtil.thumbnailOf(getPath(original).toFile(), original.getMimeType())
                .sourceRegion(region)
                .size(region.width, region.height)
                .toOutputStream(out);

            MediaFile cropped = putInPlace(
                out.getHash(), previewFormat.mimeType, tmp.path(), out.getDigest(), false
            );
            cropped = mediaFileRepository.save(cropped);

            return cropped;
        } finally {
            Files.deleteIfExists(tmp.path());
        }
    }

    private void createPreview(MediaFile original, MediaFile cropped, int width) throws IOException {
        var previewFormat = MimeUtil.thumbnail(original.getMimeType());
        if (previewFormat == null) {
            return;
        }

        MediaFilePreview largerPreview = original.findLargerPreview(width);
        if (largerPreview != null && largerPreview.getWidth() == width) {
            return;
        }

        MediaFile previewFile = cropped;
        if (cropped.getSizeX() > width) {
            var tmp = tmpFile();
            try {
                DigestingOutputStream out = new DigestingOutputStream(tmp.outputStream());

                ThumbnailUtil.thumbnailOf(getPath(cropped).toFile(), cropped.getMimeType())
                    .width(width)
                    .outputFormat(previewFormat.format)
                    .toOutputStream(out);

                long fileSize = Files.size(tmp.path());
                long prevFileSize = largerPreview != null
                    ? largerPreview.getMediaFile().getFileSize()
                    : cropped.getFileSize();
                long gain = (prevFileSize - fileSize) * 100 / prevFileSize; // negative, if fileSize > prevFileSize
                if (gain < universalContext.getOptions().getInt("media.preview-gain")) {
                    if (largerPreview != null) {
                        return;
                    }
                    // otherwise original will be used in preview
                } else {
                    previewFile = putInPlace(
                        out.getHash(), previewFormat.mimeType, tmp.path(), out.getDigest(), false
                    );
                    previewFile = mediaFileRepository.save(previewFile);
                }
            } finally {
                Files.deleteIfExists(tmp.path());
            }
        }

        MediaFilePreview preview = new MediaFilePreview();
        preview.setId(UUID.randomUUID());
        preview.setOriginalMediaFile(original);
        preview.setWidth(width);
        preview.setMediaFile(previewFile);
        preview = mediaFilePreviewRepository.save(preview);
        original.addPreview(preview);
    }

    public MediaFileOwner own(MediaFile mediaFile, String ownerName, String title) throws IOException {
        MediaFile croppedFile = cropOriginal(mediaFile);
        for (int size : PREVIEW_SIZES) {
            createPreview(mediaFile, croppedFile, size);
        }

        MediaFileOwner mediaFileOwner = new MediaFileOwner();
        mediaFileOwner.setId(UUID.randomUUID());
        mediaFileOwner.setNodeId(universalContext.nodeId());
        mediaFileOwner.setOwnerName(ownerName);
        mediaFileOwner.setTitle(title);
        mediaFileOwner.setMediaFile(mediaFile);

        if (!mediaFile.isImage()) {
            malwareListOperations.fillMalwareMarks(mediaFileOwner);
        }

        return mediaFileOwnerRepository.save(mediaFileOwner);
    }

    private Principal entryViewPrincipal(Entry entry) {
        Principal view = entry.getViewCompound();
        if (entry.getParent() != null) {
            view = view.intersect(
                entry.getParent().getViewCompound().intersect(entry.getParent().getViewCommentsCompound())
            );
        }
        return view;
    }

    public void updatePermissions(Entry entry) {
        if (entry == null) {
            return;
        }

        entryAttachmentRepository.findMediaByEntry(entry.getId()).forEach(this::updatePermissions);
    }

    public void updatePermissions(MediaFileOwner mediaFileOwner) {
        if (mediaFileOwner == null) {
            return;
        }

        Collection<Entry> entries =
            entryAttachmentRepository.findEntriesByMedia(mediaFileOwner.getNodeId(), mediaFileOwner.getId());
        mediaFileOwner.setUnrestricted(
            entries.stream()
                .map(this::entryViewPrincipal)
                .anyMatch(Principal::isPublic)
        );
        mediaFileOwner.setPermissionsUpdatedAt(Util.now());
    }

    public void deleteObsoleteMediaPostings(Collection<MediaFileOwner> mediaFileOwners) {
        mediaFileOwners.forEach(mediaFileOwner -> {
            updatePermissions(mediaFileOwner);
            postingOperations.deletePostings(obsoleteMediaPostings(mediaFileOwner));
        });
    }

    public List<Posting> obsoleteMediaPostings(Entry entry) {
        if (entry == null) {
            return Collections.emptyList();
        }

        List<Posting> obsoletePostings = new ArrayList<>();
        entryAttachmentRepository.findMediaByEntry(entry.getId())
            .forEach(mediaFileOwner -> obsoletePostings.addAll(obsoleteMediaPostings(mediaFileOwner)));
        return obsoletePostings;
    }

    private List<Posting> obsoleteMediaPostings(MediaFileOwner mediaFileOwner) {
        if (mediaFileOwner == null) {
            return Collections.emptyList();
        }

        List<Posting> obsoletePostings = new ArrayList<>();
        for (Posting posting : mediaFileOwner.getPostings()) {
            if (posting.getDeletedAt() != null) {
                continue;
            }

            Entry parent = posting.getParentMediaEntry();
            if (parent == null) {
                restrictMediaPostingPermissions(posting);
                continue;
            }
            if (parent.getDeletedAt() != null || !isAttached(mediaFileOwner, parent)) {
                obsoletePostings.add(posting);
                continue;
            }

            inheritMediaPostingPermissions(posting, parent);
        }
        return obsoletePostings;
    }

    public boolean isAttached(MediaFileOwner mediaFileOwner, Entry parent) {
        return isAttached(mediaFileOwner, parent.getId());
    }

    public boolean isAttached(MediaFileOwner mediaFileOwner, UUID parentEntryId) {
        return entryAttachmentRepository.countByEntryIdAndMedia(parentEntryId, mediaFileOwner.getId()) != 0;
    }

    private void inheritMediaPostingPermissions(Posting posting, Entry parent) {
        posting.setRejectedReactionsPositive(parent.getRejectedReactionsPositive());
        posting.setRejectedReactionsNegative(parent.getRejectedReactionsNegative());
        posting.setChildRejectedReactionsPositive(parent.getChildRejectedReactionsPositive());
        posting.setChildRejectedReactionsNegative(parent.getChildRejectedReactionsNegative());

        posting.setParentViewPrincipal(Principal.UNSET);
        posting.setViewPrincipal(entryViewPrincipal(parent));
        posting.setParentEditPrincipal(parent.getEditCompound());
        posting.setParentDeletePrincipal(parent.getDeleteCompound());
        posting.setParentViewCommentsPrincipal(Principal.UNSET);
        posting.setViewCommentsPrincipal(parent.getViewCommentsCompound());
        posting.setParentAddCommentPrincipal(Principal.UNSET);
        posting.setAddCommentPrincipal(parent.getAddCommentCompound());
        posting.setParentTrustCommentPrincipal(Principal.UNSET);
        posting.setTrustCommentPrincipal(parent.getTrustCommentCompound());
        posting.setParentOverrideCommentPrincipal(parent.getOverrideCommentCompound());
        posting.setParentViewReactionsPrincipal(Principal.UNSET);
        posting.setViewReactionsPrincipal(parent.getViewReactionsCompound());
        posting.setParentViewNegativeReactionsPrincipal(Principal.UNSET);
        posting.setViewNegativeReactionsPrincipal(parent.getViewNegativeReactionsCompound());
        posting.setParentViewReactionTotalsPrincipal(Principal.UNSET);
        posting.setViewReactionTotalsPrincipal(parent.getViewReactionTotalsCompound());
        posting.setParentViewNegativeReactionTotalsPrincipal(Principal.UNSET);
        posting.setViewNegativeReactionTotalsPrincipal(parent.getViewNegativeReactionTotalsCompound());
        posting.setParentViewReactionRatiosPrincipal(Principal.UNSET);
        posting.setViewReactionRatiosPrincipal(parent.getViewReactionRatiosCompound());
        posting.setParentViewNegativeReactionRatiosPrincipal(Principal.UNSET);
        posting.setViewNegativeReactionRatiosPrincipal(parent.getViewNegativeReactionRatiosCompound());
        posting.setParentAddReactionPrincipal(Principal.UNSET);
        posting.setAddReactionPrincipal(parent.getAddReactionCompound());
        posting.setParentAddNegativeReactionPrincipal(Principal.UNSET);
        posting.setAddNegativeReactionPrincipal(parent.getAddNegativeReactionCompound());
        posting.setParentOverrideReactionPrincipal(parent.getOverrideReactionCompound());
        posting.setParentOverrideCommentReactionPrincipal(parent.getOverrideCommentReactionCompound());
        ChildOperationsUtil.copyAll(posting, parent);
    }

    private void restrictMediaPostingPermissions(Posting posting) {
        posting.setParentViewPrincipal(Principal.UNSET);
        posting.setViewPrincipal(Principal.SECRET);
        posting.setParentEditPrincipal(Principal.UNSET);
        posting.setParentDeletePrincipal(Principal.SECRET);
        posting.setParentViewCommentsPrincipal(Principal.UNSET);
        posting.setViewCommentsPrincipal(Principal.ADMIN);
        posting.setParentAddCommentPrincipal(Principal.UNSET);
        posting.setAddCommentPrincipal(Principal.NONE);
        posting.setParentTrustCommentPrincipal(Principal.UNSET);
        posting.setTrustCommentPrincipal(Principal.NONE);
        posting.setParentOverrideCommentPrincipal(Principal.NONE);
        posting.setParentViewReactionsPrincipal(Principal.UNSET);
        posting.setViewReactionsPrincipal(Principal.ADMIN);
        posting.setParentViewNegativeReactionsPrincipal(Principal.UNSET);
        posting.setViewNegativeReactionsPrincipal(Principal.ADMIN);
        posting.setParentViewReactionTotalsPrincipal(Principal.UNSET);
        posting.setViewReactionTotalsPrincipal(Principal.ADMIN);
        posting.setParentViewNegativeReactionTotalsPrincipal(Principal.UNSET);
        posting.setViewNegativeReactionTotalsPrincipal(Principal.ADMIN);
        posting.setParentViewReactionRatiosPrincipal(Principal.UNSET);
        posting.setViewReactionRatiosPrincipal(Principal.ADMIN);
        posting.setParentViewNegativeReactionRatiosPrincipal(Principal.UNSET);
        posting.setViewNegativeReactionRatiosPrincipal(Principal.ADMIN);
        posting.setParentAddReactionPrincipal(Principal.UNSET);
        posting.setAddReactionPrincipal(Principal.NONE);
        posting.setParentAddNegativeReactionPrincipal(Principal.UNSET);
        posting.setAddNegativeReactionPrincipal(Principal.NONE);
        posting.setParentOverrideReactionPrincipal(Principal.NONE);
        posting.setParentOverrideCommentReactionPrincipal(Principal.NONE);
        ChildOperationsUtil.setRestrictive(posting);
    }

    public void mediaTextUpdated(MediaFileOwner mediaFileOwner) {
        entryRevisionRepository.clearAttachmentsCache(mediaFileOwner.getId());

        Set<UUID> updatedEntries = new HashSet<>();
        entryRevisionRepository.findByMedia(mediaFileOwner.getId()).forEach(revision -> {
            Entry entry = revision.getEntry();

            if (revision.getDeletedAt() != null || !updatedEntries.add(entry.getId())) {
                return;
            }

            switch (entry.getEntryType()) {
                case POSTING ->
                    universalContext.send(new PostingMediaTextUpdatedLiberin(
                        entry.getId(), mediaFileOwner.getId(), mediaFileOwner.getTitle(), null
                    ));
                case COMMENT ->
                    universalContext.send(new CommentMediaTextUpdatedLiberin(
                        entry.getId(), mediaFileOwner.getId(), mediaFileOwner.getTitle(), null
                    ));
            }
        });

        draftRepository.findByMedia(mediaFileOwner.getId())
            .forEach(draft -> universalContext.send(new DraftUpdatedLiberin(draft)));
    }

    public void blockMalware(MediaFileOwner mediaFileOwner, Boolean ignoreMalware) {
        if (Boolean.TRUE.equals(ignoreMalware) && !universalContext.isAdmin(Scope.VIEW_MEDIA)) {
            throw new AuthenticationException();
        }
        if (!mediaFileOwner.getMalwareMarks().isEmpty() && !Boolean.TRUE.equals(ignoreMalware)) {
            throw new OperationFailure("media.malware");
        }
    }

    public ResponseEntity<Resource> serve(MediaFile mediaFile, String title, boolean download) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(mediaFile.getMimeType()));
        if (download) {
            var builder = ContentDisposition.attachment();
            if (!ObjectUtils.isEmpty(title)) {
                builder.filename(MimeUtil.fileName(title, mediaFile.getMimeType()));
            }
            headers.setContentDisposition(builder.build());
        }
        headers.setAccessControlAllowOrigin("*");

        switch (config.getMedia().getServe().toLowerCase()) {
            default:
            case "stream": {
                headers.setContentLength(mediaFile.getFileSize());
                Path mediaPath = getPath(mediaFile);
                return new ResponseEntity<>(new FileSystemResource(mediaPath), headers, HttpStatus.OK);
            }

            case "accel":
                headers.add("X-Accel-Redirect", config.getMedia().getAccelPrefix() + mediaFile.getFileName());
                return new ResponseEntity<>(headers, HttpStatus.OK);

            case "sendfile": {
                Path mediaPath = getPath(mediaFile);
                headers.add("X-SendFile", mediaPath.toAbsolutePath().toString());
                return new ResponseEntity<>(headers, HttpStatus.OK);
            }
        }
    }

    public ResponseEntity<Resource> serve(MediaFile mediaFile, Integer width, String title, Boolean download) {
        download = download != null ? download : false;
        if (width == null) {
            return serve(mediaFile, title, download);
        }

        MediaFilePreview preview = mediaFile.findLargerPreview(width);
        return serve(preview != null ? preview.getMediaFile() : mediaFile, null, download);
    }

    public void validateAvatar(AvatarDescription avatar) {
        if (avatar == null || avatar.getMediaId() == null) {
            return;
        }

        MediaFile mediaFile = mediaFileRepository.findById(avatar.getMediaId()).orElse(null);
        if (mediaFile != null && !mediaFile.isExposed()) {
            mediaFile = null;
        }
        if (mediaFile == null && (avatar.getOptional() == null || !avatar.getOptional())) {
            throw new ObjectNotFoundFailure("avatar.not-found");
        } else {
            AvatarDescriptionUtil.setMediaFile(avatar, mediaFile);
        }
    }

    public <T> List<MediaFileOwner> validateAttachments(
        Collection<T> records,
        Function<T, String> idGetter,
        boolean compressed,
        boolean isAdminViewMedia,
        boolean isAdminUncompressedMedia,
        String clientName
    ) {
        if (ObjectUtils.isEmpty(records)) {
            return Collections.emptyList();
        }

        UUID[] uuids;
        try {
            uuids = records.stream()
                .map(idGetter)
                .map(UUID::fromString)
                .toArray(UUID[]::new);
        } catch (IllegalArgumentException e) {
            throw new ObjectNotFoundFailure("media.not-found");
        }

        return validateAttachments(uuids, compressed, isAdminViewMedia, isAdminUncompressedMedia, clientName);
    }

    public List<MediaFileOwner> validateAttachments(
        Collection<String> ids,
        boolean compressed,
        boolean isAdminViewMedia,
        boolean isAdminUncompressedMedia,
        String clientName
    ) {
        if (ObjectUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        UUID[] uuids;
        try {
            uuids = ids.stream()
                .map(UUID::fromString)
                .toArray(UUID[]::new);
        } catch (IllegalArgumentException e) {
            throw new ObjectNotFoundFailure("media.not-found");
        }

        return validateAttachments(uuids, compressed, isAdminViewMedia, isAdminUncompressedMedia, clientName);
    }

    private List<MediaFileOwner> validateAttachments(
        UUID[] ids,
        boolean compressed,
        boolean isAdminViewMedia,
        boolean isAdminUncompressedMedia,
        String clientName
    ) {
        if (ObjectUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        PostingFeatures features = PostingFeaturesUtil.build(universalContext.getOptions(), AccessCheckers.ADMIN);
        int recommendedSize = features.getImageRecommendedSize();

        List<MediaFileOwner> attached = new ArrayList<>();
        Set<UUID> usedIds = new HashSet<>();
        Map<UUID, MediaFileOwner> mediaFileOwners = mediaFileOwnerRepository.findByIds(universalContext.nodeId(), ids)
            .stream()
            .collect(Collectors.toMap(MediaFileOwner::getId, Function.identity()));
        for (UUID id : ids) {
            if (usedIds.contains(id)) {
                continue;
            }
            MediaFileOwner mediaFileOwner = mediaFileOwners.get(id);
            if (mediaFileOwner == null) {
                throw new ObjectNotFoundFailure("media.not-found");
            }
            if (
                mediaFileOwner.getOwnerName() == null && !isAdminViewMedia
                || mediaFileOwner.getOwnerName() != null && !mediaFileOwner.getOwnerName().equals(clientName)
            ) {
                throw new ObjectNotFoundFailure("media.not-found");
            }
            ValidationUtil.assertion(
                !compressed
                    || isAdminUncompressedMedia
                    || mediaFileOwner.getMediaFile().getFileSize() <= recommendedSize,
                "media.not-compressed"
            );
            attached.add(mediaFileOwner);
            usedIds.add(id);
        }
        return attached;
    }

    public List<Story> getParentStories(UUID mediaFileOwnerId) {
        Set<Entry> entries = entryRepository.findByMediaId(mediaFileOwnerId);
        return entries.stream()
            .map(entry -> entry instanceof Comment ? entry.getParent().getId() : entry.getId())
            .map(id -> storyRepository.findByEntryId(universalContext.nodeId(), id))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    @Scheduled(fixedDelayString = "PT6H")
    public void purgeUnused() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging unused media files");

            Timestamp now = Util.now();
            tx.executeWrite(() -> mediaFileOwnerRepository.deleteUnused(now));
            Collection<MediaFile> mediaFiles = tx.executeRead(() -> mediaFileRepository.findUnused(now));
            tx.executeWrite(() -> mediaFileRepository.deleteUnused(now));
            for (MediaFile mediaFile : mediaFiles) {
                Path path = getPath(mediaFile);
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("Error deleting {}: {}", path, e.getMessage());
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "PT15M")
    public void refreshPermissions() {
        try (var ignored = requestCounter.allot()) {
            log.info("Refreshing permissions of media file owners");

            for (int i = 0; i < PERMISSIONS_REFRESH_BATCHES_PER_CALL; i++) {
                boolean empty = tx.executeWrite(() -> {
                    var list = mediaFileOwnerRepository.findOutdatedPermissions(
                        Pageable.ofSize(PERMISSIONS_REFRESH_BATCH_SIZE)
                    );
                    if (list.isEmpty()) {
                        return true;
                    }
                    list.forEach(this::updatePermissions);
                    return false;
                });
                if (empty) {
                    break;
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void startRecognition() {
        if (!jobs.isReady() || !Objects.equals(config.getMedia().getOcrService(), "ocrspace")) {
            return;
        }

        try (var ignored = requestCounter.allot()) {
            log.info("Starting OCR jobs");

            List<String> ids = tx.executeRead(() -> mediaFileRepository.findToRecognize(Util.now()));
            for (String id : ids) {
                jobs.run(OcrJob.class, new OcrJob.Parameters(id));
                tx.executeWrite(() -> mediaFileRepository.assignRecognizeAt(id, null));
            }
        }
    }

}
