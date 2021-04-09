package org.moera.node.rest;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.bouncycastle.crypto.io.DigestOutputStream;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFileOwnerRepository;
import org.moera.node.data.MediaFileRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.media.BoundedOutputStream;
import org.moera.node.media.MediaPathNotSetException;
import org.moera.node.media.MimeUtils;
import org.moera.node.media.ThresholdReachedException;
import org.moera.node.model.MediaFileInfo;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.ValidationFailure;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/media")
public class MediaController {

    private static final String TMP_DIR = "tmp";

    private static Logger log = LoggerFactory.getLogger(MediaController.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private MediaFileRepository mediaFileRepository;

    @Inject
    private MediaFileOwnerRepository mediaFileOwnerRepository;

    @PostConstruct
    public void init() throws Exception {
        if (StringUtils.isEmpty(config.getMedia().getPath())) {
            throw new MediaPathNotSetException("Path not set");
        }
        try {
            Path path = FileSystems.getDefault().getPath(config.getMedia().getPath());
            if (!Files.exists(path)) {
                throw new MediaPathNotSetException("Not found");
            }
            if (!Files.isDirectory(path)) {
                throw new MediaPathNotSetException("Not a directory");
            }
            if (!Files.isWritable(path)) {
                throw new MediaPathNotSetException("Not writable");
            }
            path = path.resolve(TMP_DIR);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectory(path);
                } catch (FileAlreadyExistsException e) {
                    // ok
                } catch (Exception e) {
                    throw new MediaPathNotSetException("Cannot create tmp/ subdirectory: " + e.getMessage());
                }
            }
        } catch (InvalidPathException e) {
            throw new MediaPathNotSetException("Path is invalid");
        }
    }

    private Pair<Path, OutputStream> tmpFile() {
        while (true) {
            Path path;
            do {
                path = FileSystems.getDefault().getPath(config.getMedia().getPath(), TMP_DIR,
                        CryptoUtil.token().substring(0, 16));
            } while (Files.exists(path));
            try {
                return Pair.of(path, Files.newOutputStream(path, CREATE));
            } catch (IOException e) {
                // next try
            }
        }
    }

    @PostMapping
    @Transactional
    public MediaFileInfo post(@RequestHeader("Content-Type") String contentType,
                              @RequestHeader(value = "Content-Length", required = false) Long contentLength,
                              InputStream in) throws IOException {
        log.info("POST /media (Content-Type: {}, Content-Length: {})",
                LogUtil.format(contentType), LogUtil.format(contentLength));

        if (requestContext.getClientName() == null) {
            throw new AuthenticationException();
        }

        int maxSize = requestContext.getOptions().getInt("posting.media.max-size");
        var tmp = tmpFile();
        Path tmpPath = tmp.getFirst();
        try {
            DigestOutputStream digestStream = new DigestOutputStream(DigestFactory.getDigest("SHA-1"));
            OutputStream out = new TeeOutputStream(tmp.getSecond(), digestStream);
            if (contentLength != null) {
                if (contentLength > maxSize) {
                    throw new ValidationFailure("media.wrong-size");
                }
                in = new BoundedInputStream(in, contentLength);
            } else {
                out = new BoundedOutputStream(out, maxSize);
            }
            try {
                try {
                    in.transferTo(out);
                } catch (ThresholdReachedException e) {
                    throw new ValidationFailure("media.wrong-size");
                } catch (IOException e) {
                    throw new OperationFailure("media.storage-error");
                } finally {
                    out.close();
                }

                String id = Util.base64encode(digestStream.getDigest());
                MediaFileOwner mediaFileOwner = requestContext.isAdmin()
                        ? mediaFileOwnerRepository.findByAdminFile(requestContext.nodeId(), id).orElse(null)
                        : mediaFileOwnerRepository.findByFile(
                                requestContext.nodeId(), requestContext.getClientName(), id).orElse(null);
                if (mediaFileOwner != null) {
                    return new MediaFileInfo(mediaFileOwner);
                }

                MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
                if (mediaFile == null) {
                    String fileName = id + "." + MimeUtils.extension(contentType);
                    Path mediaPath = FileSystems.getDefault().getPath(config.getMedia().getPath(), fileName);
                    Files.move(tmpPath, mediaPath, REPLACE_EXISTING);

                    mediaFile = new MediaFile();
                    mediaFile.setId(id);
                    mediaFile.setMimeType(contentType);
                    mediaFile.setFileSize(Files.size(mediaPath));
                    mediaFile = mediaFileRepository.save(mediaFile);
                }

                mediaFileOwner = new MediaFileOwner();
                mediaFileOwner.setId(UUID.randomUUID());
                mediaFileOwner.setNodeId(requestContext.nodeId());
                mediaFileOwner.setOwnerName(requestContext.isAdmin() ? null : requestContext.getClientName());
                mediaFileOwner.setMediaFile(mediaFile);
                mediaFileOwner = mediaFileOwnerRepository.save(mediaFileOwner);

                return new MediaFileInfo(mediaFileOwner);
            } catch (IOException e) {
                throw new OperationFailure("media.storage-error");
            }
        } finally {
            Files.deleteIfExists(tmpPath);
        }
    }

}
