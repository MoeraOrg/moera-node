package org.moera.node.media;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ImageScalerTest {

    private record JpegFrame(int marker, int[] samplingFactors) {
    }

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setUp() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void jpegQualityIsEstimatedFromSourceFile() throws IOException {
        Path source = tempDir.resolve("source.jpg");
        writeJpeg(noisyImage(320, 240), source, 0.76f);

        Assertions.assertEquals(0.76f, JpegUtil.estimateJpegQuality(source.toFile()), 0.08f);
        Assertions.assertEquals(0.85f, JpegUtil.DEFAULT_JPEG_QUALITY, 0.001f);
        Assertions.assertTrue(MimeUtil.isJpeg("image/pjpeg"));
        Assertions.assertFalse(MimeUtil.isJpeg("image/png"));
    }

    @Test
    void downsizeReducesImageCloseToTargetSize() throws IOException {
        int width = 1200;
        int height = 900;
        Path source = tempDir.resolve("source.jpg");
        Path target = tempDir.resolve("target.jpg");
        writeJpeg(noisyImage(width, height), source, 0.92f);
        long sourceSize = Files.size(source);
        long targetSize = sourceSize / 3;

        String mimeType = ImageScaler.downsize(
            source.toFile(),
            target.toFile(),
            "image/jpeg",
            new Dimension(width, height),
            sourceSize,
            targetSize
        );

        long downsizedSize = Files.size(target);
        BufferedImage downsized = ImageIO.read(target.toFile());
        Assertions.assertEquals("image/jpeg", mimeType);
        Assertions.assertTrue(downsizedSize < sourceSize);
        Assertions.assertTrue(downsizedSize <= targetSize + Math.round(targetSize * 0.05));
        Assertions.assertTrue(downsized.getWidth() < width);
        Assertions.assertTrue(downsized.getHeight() < height);

        JpegFrame frame = readJpegFrame(target);
        Assertions.assertEquals(0xc2, frame.marker());
        Assertions.assertArrayEquals(new int[] {0x22, 0x11, 0x11}, frame.samplingFactors());
    }

    private static BufferedImage noisyImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = x * 37 + y * 17 & 0xff;
                int green = x * 13 + y * 43 & 0xff;
                int blue = x * 67 + y * 29 & 0xff;
                image.setRGB(x, y, red << 16 | green << 8 | blue);
            }
        }
        return image;
    }

    private static void writeJpeg(BufferedImage image, Path path, float quality) throws IOException {
        var writer = ImageIO.getImageWritersByFormatName("JPEG").next();
        var param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (var out = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private static JpegFrame readJpegFrame(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        if (data.length < 4 || (data[0] & 0xff) != 0xff || (data[1] & 0xff) != 0xd8) {
            throw new IOException("Not a JPEG file");
        }

        int offset = 2;
        while (offset < data.length) {
            while (offset < data.length && (data[offset] & 0xff) != 0xff) {
                offset++;
            }
            while (offset < data.length && (data[offset] & 0xff) == 0xff) {
                offset++;
            }
            if (offset >= data.length) {
                break;
            }

            int marker = data[offset++] & 0xff;
            if (marker == 0xd9 || marker == 0xda) {
                break;
            }
            if (isStandaloneJpegMarker(marker)) {
                continue;
            }
            if (offset + 2 > data.length) {
                break;
            }

            int length = ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
            if (length < 2 || offset + length > data.length) {
                break;
            }
            if (isStartOfFrameMarker(marker)) {
                if (length < 8) {
                    break;
                }
                int components = data[offset + 7] & 0xff;
                if (length < 8 + 3 * components) {
                    break;
                }
                int[] samplingFactors = new int[components];
                for (int i = 0; i < components; i++) {
                    samplingFactors[i] = data[offset + 9 + 3 * i] & 0xff;
                }
                return new JpegFrame(marker, samplingFactors);
            }
            offset += length;
        }

        throw new IOException("JPEG frame marker not found");
    }

    private static boolean isStandaloneJpegMarker(int marker) {
        return marker == 0x01 || marker >= 0xd0 && marker <= 0xd8;
    }

    private static boolean isStartOfFrameMarker(int marker) {
        return marker >= 0xc0 && marker <= 0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc;
    }

}
