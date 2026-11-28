package com.barboya.files;

import com.barboya.common.ApiException;
import org.springframework.http.HttpStatus;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Convierte una foto publica en un JPEG nuevo de tamano moderado. Al volver a
 * crear la imagen se descartan los metadatos, incluida la ubicacion donde se tomo.
 */
public final class ProductPhotos {

    static final int MAX_SIDE = 1000;
    private static final float QUALITY = 0.85f;

    private ProductPhotos() {
    }

    /** @param image JPG o PNG ya validado con {@link Uploads} */
    public static byte[] normalize(byte[] image) {
        try {
            return write(render(decode(image), ExifOrientation.of(image)));
        } catch (IOException | RuntimeException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE", "La imagen esta dañada");
        }
    }

    private static BufferedImage decode(byte[] image) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            try {
                reader.setInput(input);
                // Las fotos enormes se leen salteando pixeles para no agotar la memoria
                int step = Math.max(1, Math.max(reader.getWidth(0), reader.getHeight(0)) / (MAX_SIDE * 2));
                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(step, step, 0, 0);
                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage render(BufferedImage source, int orientation) {
        int w = source.getWidth();
        int h = source.getHeight();
        boolean turned = orientation >= 5;
        double scale = Math.min(1.0, (double) MAX_SIDE / Math.max(w, h));
        int width = Math.max(1, (int) Math.round((turned ? h : w) * scale));
        int height = Math.max(1, (int) Math.round((turned ? w : h) * scale));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            // El fondo transparente de un PNG quedaria negro en JPEG
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.scale(scale, scale);
            graphics.drawImage(source, upright(orientation, w, h), null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    /** Gira o refleja segun como sostenia el telefono quien tomo la foto. */
    private static AffineTransform upright(int orientation, int w, int h) {
        return switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
            default -> new AffineTransform();
        };
    }

    private static byte[] write(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
