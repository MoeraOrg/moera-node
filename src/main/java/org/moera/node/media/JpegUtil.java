package org.moera.node.media;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGQTable;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class JpegUtil {

    public static final float DEFAULT_JPEG_QUALITY = 0.85f;

    private static final String JPEG_METADATA_FORMAT = "javax_imageio_jpeg_image_1.0";

    /*
     * JPEG compression discards visual detail by dividing image frequencies by quantization table values and rounding
     * the result. Larger table values mean more detail is rounded away, so they correspond to lower encoder quality.
     * Java exposes the tables embedded in the JPEG metadata. We compare each embedded table with the standard JPEG
     * luminance/chrominance tables, derive the scale factor that would have produced it, convert that scale back to the
     * usual 1..100 quality range used by JPEG encoders, and average the result across all available tables.
     */
    public static Float estimateJpegQuality(File file) {
        var readers = ImageIO.getImageReadersByFormatName("JPEG");
        if (!readers.hasNext()) {
            return null;
        }

        ImageReader reader = readers.next();
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (input == null) {
                return null;
            }
            reader.setInput(input, true, true);

            var metadata = reader.getImageMetadata(0);
            var root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
            var nodes = root.getElementsByTagName("dqtable");
            List<Float> qualities = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                var node = (IIOMetadataNode) nodes.item(i);
                if (node.getUserObject() instanceof JPEGQTable table) {
                    int[] standard = "0".equals(node.getAttribute("qtableId"))
                        ? JPEGQTable.K1Luminance.getTable()
                        : JPEGQTable.K2Chrominance.getTable();
                    qualities.add(estimateJpegQuality(table.getTable(), standard));
                }
            }

            if (qualities.isEmpty()) {
                return null;
            }
            float sum = 0;
            for (float quality : qualities) {
                sum += quality;
            }
            return sum / qualities.size();
        } catch (Exception e) {
            return null;
        } finally {
            reader.dispose();
        }
    }

    private static float estimateJpegQuality(int[] table, int[] standard) {
        double scale = 0;
        int count = Math.min(table.length, standard.length);
        for (int i = 0; i < count; i++) {
            scale += (double) table[i] * 100 / standard[i];
        }
        scale /= count;

        double quality = scale <= 100 ? (200 - scale) / 2 : 5000 / scale;
        return (float) Math.clamp(quality / 100, 0.01, 1);
    }

    public static void writeJpeg(BufferedImage image, OutputStream target, float quality) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("JPEG");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG image writer found");
        }

        var writer = writers.next();
        var rgbImage = toRgbImage(image);
        var param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        if (!param.canWriteProgressive()) {
            throw new IOException("JPEG image writer does not support progressive output");
        }
        param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);

        var metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(rgbImage), param);
        setJpeg420(metadata);

        try (ImageOutputStream out = ImageIO.createImageOutputStream(target)) {
            if (out == null) {
                throw new IOException("Could not open image output stream");
            }
            writer.setOutput(out);
            writer.write(null, new IIOImage(rgbImage, null, metadata), param);
        } finally {
            writer.dispose();
        }
    }

    private static BufferedImage toRgbImage(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB) {
            return image;
        }

        var rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgbImage;
    }

    private static void setJpeg420(IIOMetadata metadata) throws IOException {
        var root = (IIOMetadataNode) metadata.getAsTree(JPEG_METADATA_FORMAT);
        var markerSequence = (IIOMetadataNode) root.getElementsByTagName("markerSequence").item(0);
        var sof = (IIOMetadataNode) markerSequence.getElementsByTagName("sof").item(0);
        sof.setAttribute("process", "2");
        sof.setAttribute("numFrameComponents", "3");
        while (sof.hasChildNodes()) {
            sof.removeChild(sof.getFirstChild());
        }
        sof.appendChild(componentSpec("1", "2", "2", "0"));
        sof.appendChild(componentSpec("2", "1", "1", "1"));
        sof.appendChild(componentSpec("3", "1", "1", "1"));
        metadata.setFromTree(JPEG_METADATA_FORMAT, root);
    }

    private static IIOMetadataNode componentSpec(
        String componentId, String hSamplingFactor, String vSamplingFactor, String qTableSelector
    ) {
        var component = new IIOMetadataNode("componentSpec");
        component.setAttribute("componentId", componentId);
        component.setAttribute("HsamplingFactor", hSamplingFactor);
        component.setAttribute("VsamplingFactor", vSamplingFactor);
        component.setAttribute("QtableSelector", qTableSelector);
        return component;
    }

}
