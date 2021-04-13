package org.moera.node.media;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.LogUtil;
import org.moera.node.config.Config;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

@Component
public class MediaOperations {

    public static final String TMP_DIR = "tmp";

    private static Logger log = LoggerFactory.getLogger(MediaOperations.class);

    @Inject
    private Config config;

    @Inject
    private MediaFileRepository mediaFileRepository;

    public Pair<Path, OutputStream> tmpFile() {
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

    private Dimension getImageDimension(String contentType, Path path) {
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

        return null;
    }

    public MediaFile putInPlace(String id, String contentType, Path tmpPath) throws IOException {
        MediaFile mediaFile = mediaFileRepository.findById(id).orElse(null);
        if (mediaFile == null) {
            Path mediaPath = FileSystems.getDefault().getPath(
                    config.getMedia().getPath(), MimeUtils.fileName(id, contentType));
            Files.move(tmpPath, mediaPath, REPLACE_EXISTING);

            mediaFile = new MediaFile();
            mediaFile.setId(id);
            mediaFile.setMimeType(contentType);
            mediaFile.setDimension(getImageDimension(contentType, mediaPath));
            mediaFile.setFileSize(Files.size(mediaPath));
            mediaFile = mediaFileRepository.save(mediaFile);
        }
        return mediaFile;
    }

}
