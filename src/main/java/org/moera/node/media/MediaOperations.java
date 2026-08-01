package org.moera.node.media;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.node.types.AvatarDescription;
import org.moera.lib.node.types.MediaToAttach;
import org.moera.lib.node.types.RemoteMedia;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.config.Config;
import org.moera.node.data.ChildOperationsUtil;
import org.moera.node.data.DraftRepository;
import org.moera.node.data.Entry;
import org.moera.node.data.EntryAttachment;
import org.moera.node.data.EntryAttachmentRepository;
import org.moera.node.data.EntryRevisionRepository;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.data.MediaFilePreviewRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.data.MediaLease;
import org.moera.node.data.MediaLeaseRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.RemoteMediaFile;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.CommentMediaTextUpdatedLiberin;
import org.moera.node.liberin.model.DraftUpdatedLiberin;
import org.moera.node.liberin.model.PostingMediaTextUpdatedLiberin;
import org.moera.node.media.awss3.AwsS3MediaStorage;
import org.moera.node.media.image.ImageScaler;
import org.moera.node.media.image.ImageUtil;
import org.moera.node.media.image.InvalidImageException;
import org.moera.node.media.image.ThumbnailUtil;
import org.moera.node.media.video.VideoUtil;
import org.moera.node.model.AvatarDescriptionUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.RemoteMediaUtil;
import org.moera.node.operations.OcrJob;
import org.moera.node.operations.PostingOperations;
import org.moera.node.task.Jobs;
import org.moera.node.userlist.MalwareListOperations;
import org.moera.node.util.DigestingOutputStream;
import org.moera.node.util.ExtendedDuration;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Component
public class MediaOperations {

    public static final String TMP_DIR = "tmp";

    private static final Logger log = LoggerFactory.getLogger(MediaOperations.class);

    private static final int[] PREVIEW_SIZES = {1400, 900, 150};

    private static final int PERMISSIONS_REFRESH_BATCH_SIZE = 100;
    private static final int PERMISSIONS_REFRESH_BATCHES_PER_CALL = 5;

    private class PreviewSource {

        private final MediaFile mediaFile;
        private BufferedImage image;

        PreviewSource(MediaFile mediaFile) {
            this.mediaFile = mediaFile;
        }

        PreviewSource(MediaFile mediaFile, BufferedImage image) {
            this.mediaFile = mediaFile;
            this.image = image;
        }

        public MediaFile getMediaFile() {
            return mediaFile;
        }

        public BufferedImage loadImage() throws IOException {
            if (image == null) {
                image = readImage(mediaFile);
            }
            return image;
        }

        private BufferedImage readImage(MediaFile mediaFile) throws IOException {
            try (var content = openContent(mediaFile)) {
                return ThumbnailUtil.thumbnailOf(content.path().toFile(), mediaFile.getMimeType())
                    .scale(1)
                    .useExifOrientation(true)
                    .asBufferedImage();
            }
        }

        public PreviewSource from(MediaFile mediaFile) {
            if (Objects.equals(this.mediaFile.getId(), mediaFile.getId())) {
                return this;
            }
            return new PreviewSource(mediaFile);
        }

        public PreviewSource or(MediaFilePreview largerPreview) {
            if (largerPreview != null) {
                return from(largerPreview.getMediaFile());
            }
            return this;
        }

    }

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Config config;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private AwsS3MediaStorage awsS3MediaStorage;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @Inject
    private MediaFilePreviewRepository mediaFilePreviewRepository;

    @Inject
    private MediaLeaseRepository mediaLeaseRepository;

    @Inject
    private EntryAttachmentRepository entryAttachmentRepository;

    @Inject
    private EntryRevisionRepository entryRevisionRepository;

    @Inject
    private DraftRepository draftRepository;

    @Inject
    private DirectServeOperations directServeOperations;

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

    public Path getPath(MediaFile mediaFile) throws MediaFileNotAvailableException {
        return getPath(mediaFile.getId(), mediaFile.getFileName());
    }

    public Path getPath(String id, String fileName) throws MediaFileNotAvailableException {
        if (ObjectUtils.isEmpty(fileName)) {
            throw new MediaFileNotAvailableException(id);
        }
        return FileSystems.getDefault().getPath(config.getMedia().getPath(), fileName);
    }

    public MediaFileContent openContent(MediaFile mediaFile) throws MediaFileNotAvailableException {
        if (!ObjectUtils.isEmpty(mediaFile.getFileName())) {
            return new MediaFileContent(getPath(mediaFile), false);
        }
        if (ObjectUtils.isEmpty(mediaFile.getCloudFileName()) || !awsS3MediaStorage.isConfigured()) {
            throw new MediaFileNotAvailableException(mediaFile.getId());
        }

        TemporaryFile tmp = tmpFile();
        try {
            tmp.outputStream().close();
            var download = awsS3MediaStorage.download(mediaFile.getCloudFileName(), tmp.path());
            try {
                download.get();
            } catch (InterruptedException e) {
                download.cancel(true);
                Thread.currentThread().interrupt();
                throw e;
            }
            return new MediaFileContent(tmp.path(), true);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tmp.path());
            } catch (IOException ex) {
                e.addSuppressed(ex);
            }
            throw new MediaFileNotAvailableException(mediaFile.getId(), e);
        }
    }

    public DigestingOutputStream transfer(
        InputStream in, OutputStream out, Long contentLength, Integer maxSize
    ) throws IOException {
        if (maxSize == null) {
            maxSize = universalContext.getOptions().getInt("media.max-size");
        }

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

    public String downsizeImage(Path path, String contentType) throws IOException {
        return downsizeImage(path, contentType, universalContext.getOptions().getInt("media.image.recommended-size"));
    }

    public String downsizeImage(Path path, String contentType, long targetSize) throws IOException {
        contentType = detectContentType(path, contentType);
        if (!MimeUtil.isSupportedImage(contentType)) {
            return contentType;
        }

        long fileSize = Files.size(path);
        if (fileSize <= targetSize) {
            return contentType;
        }

        Dimension dimension = ImageUtil.getImageDimension(contentType, path);
        if (!MimeUtil.isReasonableImageForDownsize(contentType, dimension.width, dimension.height, fileSize)) {
            return contentType;
        }

        var tmp = tmpFile();
        try {
            tmp.outputStream().close();
            String downsizedContentType = ImageScaler.downsize(
                path.toFile(), tmp.path().toFile(), contentType, dimension, fileSize, targetSize
            );
            if (Files.size(tmp.path()) >= fileSize) {
                return contentType;
            }
            Files.move(tmp.path(), path, REPLACE_EXISTING);
            return downsizedContentType;
        } finally {
            Files.deleteIfExists(tmp.path());
        }
    }

    @Transactional(REQUIRES_NEW)
    public MediaFile putInPlace(
        String id, String contentType, Path tmpPath, byte[] digest, boolean exposed
    ) throws IOException {
        mediaFileRepository.lockMediaFileId(id);
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null) {
            if (digest == null) {
                digest = digest(tmpPath);
            }
            // image type may be wrong, autodetection will give more accurate result
            contentType = detectContentType(tmpPath, contentType);
            if (exposed && !MimeUtil.isSupportedImage(contentType)) {
                throw new InvalidImageException();
            }

            String fileName = MimeUtil.fileName(id, contentType);
            Path mediaPath = FileSystems.getDefault().getPath(config.getMedia().getPath(), fileName);
            Files.move(tmpPath, mediaPath, REPLACE_EXISTING);

            mediaFile = new MediaFile();
            mediaFile.setId(id);
            mediaFile.setMimeType(contentType);
            mediaFile.setFileName(fileName);
            if (MimeUtil.isSupportedImage(contentType)) {
                mediaFile.setDimension(ImageUtil.getImageDimension(contentType, mediaPath));
                mediaFile.setOrientation(ImageUtil.getImageOrientation(mediaPath));
            } else if (MimeUtil.isSupportedVideo(contentType)) {
                var videoInfo = VideoUtil.getVideoInfo(mediaPath);
                mediaFile.setDimension(videoInfo.dimension());
                mediaFile.setDuration(videoInfo.duration());
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

    private String detectContentType(Path path, String contentType) throws IOException {
        try (var in = new BufferedInputStream(Files.newInputStream(path))) {
            FileType fileType = FileTypeDetector.detectFileType(in);
            String candidateType = fileType != FileType.Unknown ? fileType.getMimeType() : null;
            if (candidateType != null && candidateType.startsWith("image/")) {
                return candidateType;
            }

            var metadata = new Metadata();
            if (!isEmptyContentType(contentType)) {
                metadata.set(Metadata.CONTENT_TYPE, contentType);
            }
            candidateType = new Tika().detect(in, metadata);
            if (!isEmptyContentType(candidateType)) {
                return candidateType;
            }

            return Objects.requireNonNullElse(contentType, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        }
    }

    private boolean isEmptyContentType(String contentType) {
        return !StringUtils.hasText(contentType)
            || MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(contentType);
    }

    public byte[] digest(MediaFile mediaFile) throws IOException {
        try (var content = openContent(mediaFile)) {
            return digest(content.path());
        }
    }

    private static byte[] digest(Path mediaPath) throws IOException {
        DigestingOutputStream out = new DigestingOutputStream(OutputStream.nullOutputStream());
        try (InputStream in = Files.newInputStream(mediaPath)) {
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

    /**
     * Crop the original image for preview. Images that are too wide or too high need to be cropped for preview.
     *
     * @param original the original image
     * @return the cropped image (may be the same as the original
     * @throws IOException
     */
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

            try (var content = openContent(original)) {
                ThumbnailUtil.thumbnailOf(content.path().toFile(), original.getMimeType())
                    .sourceRegion(region)
                    .size(region.width, region.height)
                    .toOutputStream(out);
            }

            MediaFile cropped = putInPlace(
                out.getHash(), previewFormat.mimeType(), tmp.path(), out.getDigest(), false
            );
            cropped = mediaFileRepository.save(cropped);

            return cropped;
        } finally {
            Files.deleteIfExists(tmp.path());
        }
    }

    private PreviewSource createPreview(
        MediaFile original, MediaFile cropped, PreviewSource previewSource, int width
    ) throws IOException {
        var previewFormat = original.isImage() ? MimeUtil.thumbnail(original.getMimeType()) : MimeUtil.JPEG;
        if (previewFormat == null) {
            return previewSource;
        }

        MediaFilePreview largerPreview = original.findLargerPreview(width);
        PreviewSource source = previewSource != null ? previewSource : new PreviewSource(cropped);
        if (largerPreview != null && largerPreview.getWidth() == width) {
            return source.from(largerPreview.getMediaFile());
        }

        source = source.or(largerPreview);
        MediaFile previewFile = source.getMediaFile();
        if (!source.getMediaFile().isImage() || source.getMediaFile().getSizeX() > width) {
            var tmp = tmpFile();
            try {
                PreviewResult result = source.getMediaFile().isImage()
                    ? createImagePreview(source, previewFormat, width, tmp)
                    : createVideoPreview(source, width, tmp);
                try (var digests = result.digests()) {
                    if (result.previewImage() == null) {
                        if (largerPreview != null) {
                            return source;
                        }
                        // otherwise original will be used in preview
                    } else {
                        previewFile = putInPlace(
                            digests.getHash(), previewFormat.mimeType(), tmp.path(), digests.getDigest(), false
                        );
                        previewFile = mediaFileRepository.save(previewFile);
                        source = new PreviewSource(previewFile, result.previewImage());
                    }
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

        return source.from(previewFile);
    }

    private record PreviewResult(DigestingOutputStream digests, BufferedImage previewImage) {
    }

    private PreviewResult createImagePreview(
        PreviewSource source,
        MimeUtil.ThumbnailFormat previewFormat,
        int width,
        TemporaryFile destination
    ) throws IOException {
        DigestingOutputStream out = new DigestingOutputStream(destination.outputStream());

        BufferedImage previewImage = Thumbnails.of(source.loadImage())
            .width(width)
            .asBufferedImage();
        ThumbnailUtil.toOutputStream(Thumbnails.of(previewImage).scale(1), out, previewFormat);

        long fileSize = Files.size(destination.path());
        long prevFileSize = source.getMediaFile().getFileSize();
        long gain = (prevFileSize - fileSize) * 100 / prevFileSize; // negative, if fileSize > prevFileSize
        boolean worthIt = gain >= universalContext.getOptions().getInt("media.preview-gain");

        return new PreviewResult(out, worthIt ? previewImage : null);
    }

    private PreviewResult createVideoPreview(
        PreviewSource source,
        int width,
        TemporaryFile destination
    ) throws IOException {
        destination.outputStream().close();

        try (var content = openContent(source.getMediaFile())) {
            VideoUtil.thumbnailVideo(content.path(), width, destination.path());
        }

        DigestingOutputStream out;
        try (InputStream tmpIn = Files.newInputStream(destination.path())) {
            out = transfer(tmpIn, null, null, null);
        }

        return new PreviewResult(out, ImageIO.read(destination.path().toFile()));
    }

    public MediaFileOwner own(MediaFile mediaFile, String title) throws IOException {
        if (mediaFile.isReasonableImage() || mediaFile.isVideo()) {
            MediaFile croppedFile = mediaFile.isImage() ? cropOriginal(mediaFile) : mediaFile;
            PreviewSource previewSource = null;
            for (int size : PREVIEW_SIZES) {
                previewSource = createPreview(mediaFile, croppedFile, previewSource, size);
            }
        }

        MediaFileOwner mediaFileOwner = new MediaFileOwner();
        mediaFileOwner.setId(UUID.randomUUID());
        mediaFileOwner.setNodeId(universalContext.nodeId());
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
                updateMediaPostingPermissions(posting);
                continue;
            }
            if (parent.getDeletedAt() != null || !isAttached(mediaFileOwner, parent)) {
                obsoletePostings.add(posting);
                continue;
            }

            updateMediaPostingPermissions(posting);
        }
        return obsoletePostings;
    }

    public boolean isAttached(MediaFileOwner mediaFileOwner, Entry parent) {
        return isAttached(mediaFileOwner, parent.getId());
    }

    public boolean isAttached(MediaFileOwner mediaFileOwner, UUID parentEntryId) {
        return entryAttachmentRepository.countByEntryIdAndMedia(parentEntryId, mediaFileOwner.getId()) != 0;
    }

    public void updateMediaPostingPermissions(Posting posting) {
        Entry parent = posting.getParentMediaEntry();
        if (parent != null) {
            inheritMediaPostingPermissions(posting, parent);
        } else {
            restrictMediaPostingPermissions(posting);
        }
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
        entryRevisionRepository.clearAttachmentsCacheByMedia(mediaFileOwner.getId());

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
        if (Boolean.TRUE.equals(ignoreMalware) && !universalContext.isAdmin(Scope.VIEW_CONTENT)) {
            throw new AuthenticationException();
        }
        if (!mediaFileOwner.getMalwareMarks().isEmpty() && !Boolean.TRUE.equals(ignoreMalware)) {
            throw new OperationFailure("media.malware");
        }
    }

    private void setCacheControl(HttpHeaders headers, ExtendedDuration cacheDuration) {
        CacheControl cacheControl = switch (cacheDuration.getZone()) {
            case ALWAYS -> CacheControl.maxAge(3650, TimeUnit.DAYS);
            case NEVER -> CacheControl.noStore();
            case FIXED -> CacheControl.maxAge(cacheDuration.getSeconds(), TimeUnit.SECONDS);
        };
        headers.setCacheControl(cacheControl);
    }

    private ResponseEntity<Resource> serve(
        MediaFile mediaFile, String fileName, boolean download, ExtendedDuration cacheDuration
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(mediaFile.getMimeType()));
        setCacheControl(headers, cacheDuration);
        if (download) {
            var builder = ContentDisposition.attachment();
            if (!ObjectUtils.isEmpty(fileName)) {
                builder.filename(fileName);
            }
            headers.setContentDisposition(builder.build());
        }
        headers.setAccessControlAllowOrigin("*");

        try {
            if (!ObjectUtils.isEmpty(mediaFile.getFileName())) {
                Path mediaPath = getPath(mediaFile);
                return switch (config.getMedia().getServe()) {
                    case STREAM -> {
                        headers.setContentLength(mediaFile.getFileSize());
                        yield new ResponseEntity<>(new FileSystemResource(mediaPath), headers, HttpStatus.OK);
                    }
                    case ACCEL -> {
                        headers.add(
                            "X-Accel-Redirect",
                            Util.endWithSlash(config.getMedia().getAccelPrefix()) + mediaFile.getFileName()
                        );
                        yield new ResponseEntity<>(headers, HttpStatus.OK);
                    }
                    case SENDFILE -> {
                        headers.add("X-SendFile", mediaPath.toAbsolutePath().toString());
                        yield new ResponseEntity<>(headers, HttpStatus.OK);
                    }
                };
            }
        } catch (MediaFileNotAvailableException e) {
            // ignore
        }

        if (!ObjectUtils.isEmpty(mediaFile.getCloudFileName())) {
            return switch (config.getMedia().getServe()) {
                case STREAM, SENDFILE -> {
                    headers.set(HttpHeaders.LOCATION, directServeOperations.directUrl(mediaFile));
                    yield new ResponseEntity<>(headers, HttpStatus.FOUND);
                }
                case ACCEL -> {
                    headers.add(
                        "X-Accel-Redirect",
                        Util.endWithNoSlash(config.getMedia().getCloudAccelPrefix())
                            + directServeOperations.directLocation(mediaFile)
                    );
                    yield new ResponseEntity<>(headers, HttpStatus.OK);
                }
            };
        }

        throw new ObjectNotFoundFailure("media.not-found");
    }

    public ResponseEntity<Resource> serve(
        MediaFile mediaFile, Integer width, String fileName, Boolean download, ExtendedDuration cacheDuration
    ) {
        download = download != null ? download : false;
        if (width == null) {
            return serve(mediaFile, fileName, download, cacheDuration);
        }

        MediaFilePreview preview = mediaFile.findLargerPreview(width);
        return serve(preview != null ? preview.getMediaFile() : mediaFile, null, download, cacheDuration);
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

    public List<LocalRemoteMedia> validateAttachments(
        Collection<MediaToAttach> mediaList,
        Collection<EntryAttachment> prevMediaList,
        boolean isAdminViewMedia
    ) {
        if (ObjectUtils.isEmpty(mediaList)) {
            return Collections.emptyList();
        }

        List<LocalRemoteMedia> attached = new ArrayList<>();
        Set<UUID> usedIds = prevMediaList != null
            ? prevMediaList.stream()
                .map(EntryAttachment::getMediaFileOwner)
                .filter(Objects::nonNull)
                .map(MediaFileOwner::getId)
                .collect(Collectors.toSet())
            : new HashSet<>();
        UUID[] localMediaIds = mediaList.stream()
            .map(MediaToAttach::getLocalMediaId)
            .filter(Objects::nonNull)
            .map(UUID::fromString)
            .toArray(UUID[]::new);
        Map<UUID, MediaFileOwner> mediaFileOwners = mediaFileOwnerRepository
            .findByIds(universalContext.nodeId(), localMediaIds)
            .stream()
            .collect(Collectors.toMap(MediaFileOwner::getId, Function.identity()));
        for (MediaToAttach media : mediaList) {
            MediaLease mediaLease = null;
            if (media.getLocalMediaLeaseId() != null) {
                UUID leaseId = Util.uuid(media.getLocalMediaLeaseId())
                    .orElseThrow(() -> new ObjectNotFoundFailure("media-lease.not-found"));
                mediaLease = mediaLeaseRepository.findByNodeIdAndId(universalContext.nodeId(), leaseId)
                    .orElseThrow(() -> new ObjectNotFoundFailure("media-lease.not-found"));
            }

            RemoteMediaFile remoteMediaFile = media.getRemoteMedia() != null
                ? RemoteMediaUtil.toNewRemoteMediaFile(
                    universalContext.nodeId(), media.getRemoteMedia()
                )
                : null;
            if (media.getLocalMediaId() == null) {
                if (remoteMediaFile != null) {
                    attached.add(new LocalRemoteMedia(null, remoteMediaFile, mediaLease));
                }
                continue;
            }

            UUID localMediaId = Util.uuid(media.getLocalMediaId())
                .orElseThrow(() -> new ObjectNotFoundFailure("media.not-found"));
            MediaFileOwner mediaFileOwner = mediaFileOwners.get(localMediaId);
            if (!usedIds.contains(localMediaId) && (mediaFileOwner == null || !isAdminViewMedia)) {
                throw new ObjectNotFoundFailure("media.not-found");
            }
            attached.add(new LocalRemoteMedia(mediaFileOwner, remoteMediaFile, mediaLease));
            usedIds.add(localMediaId);
        }
        return attached;
    }

    public void clearDraftOnlyMediaLeases(Collection<MediaToAttach> attachments) {
        if (ObjectUtils.isEmpty(attachments)) {
            return;
        }

        Set<UUID> leaseIds = attachments.stream()
            .map(MediaToAttach::getRemoteMedia)
            .filter(Objects::nonNull)
            .filter(rm -> Objects.equals(rm.getNodeName(), universalContext.nodeName()))
            .map(RemoteMedia::getLeaseId)
            .filter(Objects::nonNull)
            .map(Util::uuid)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());

        if (leaseIds.isEmpty()) {
            return;
        }

        tx.executeWrite(() -> mediaLeaseRepository.clearDraftOnly(universalContext.nodeId(), leaseIds));
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
