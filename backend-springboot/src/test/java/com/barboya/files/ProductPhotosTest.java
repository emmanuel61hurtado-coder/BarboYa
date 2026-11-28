package com.barboya.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPhotosTest {

    /** Imagen azul con un cuadro rojo en la esquina superior izquierda. */
    private static byte[] marked(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, width / 4, height / 4);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    /** Inserta metadatos EXIF con la orientacion y un texto que simula datos privados. */
    private static byte[] withExif(byte[] jpeg, int orientation) {
        byte[] secret = "GPS-5.9333,-73.6153".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer tiff = ByteBuffer.allocate(26 + secret.length);
        tiff.put("MM".getBytes(StandardCharsets.US_ASCII)).putShort((short) 42).putInt(8);
        tiff.putShort((short) 1);
        tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0);
        tiff.putInt(0).put(secret);
        ByteBuffer out = ByteBuffer.allocate(jpeg.length + 10 + tiff.capacity());
        out.put(jpeg, 0, 2);
        out.putShort((short) 0xFFE1).putShort((short) (8 + tiff.capacity()));
        out.put("Exif\0\0".getBytes(StandardCharsets.US_ASCII)).put(tiff.array());
        out.put(jpeg, 2, jpeg.length - 2);
        return out.array();
    }

    private static BufferedImage read(byte[] content) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(content));
    }

    private static boolean isRed(BufferedImage image, int x, int y) {
        Color color = new Color(image.getRGB(x, y));
        return color.getRed() > 180 && color.getBlue() < 90;
    }

    @Test
    void largePhotosAreReducedAndComeOutAsJpeg() throws IOException {
        byte[] result = ProductPhotos.normalize(marked(4800, 3200, "png"));

        BufferedImage image = read(result);
        assertThat(image.getWidth()).isEqualTo(1000);
        assertThat(image.getHeight()).isBetween(666, 667);
        assertThat(result[0]).isEqualTo((byte) 0xFF);
        assertThat(result[1]).isEqualTo((byte) 0xD8);
        assertThat(isRed(image, 20, 20)).isTrue();
    }

    @Test
    void smallPhotosKeepTheirSize() throws IOException {
        BufferedImage image = read(ProductPhotos.normalize(marked(120, 80, "jpg")));

        assertThat(image.getWidth()).isEqualTo(120);
        assertThat(image.getHeight()).isEqualTo(80);
    }

    /** Donde queda la esquina marcada segun cada orientacion: 0 es izquierda o arriba. */
    @ParameterizedTest
    @CsvSource({"1,false,0,0", "2,false,1,0", "3,false,1,1", "4,false,0,1",
            "5,true,0,0", "6,true,1,0", "7,true,1,1", "8,true,0,1"})
    void thePhotoIsTurnedUprightAndLosesItsMetadata(int orientation, boolean turned, int right, int bottom)
            throws IOException {
        byte[] result = ProductPhotos.normalize(withExif(marked(160, 80, "jpg"), orientation));

        BufferedImage image = read(result);
        assertThat(image.getWidth()).isEqualTo(turned ? 80 : 160);
        assertThat(image.getHeight()).isEqualTo(turned ? 160 : 80);
        assertThat(isRed(image, right == 0 ? 5 : image.getWidth() - 6, bottom == 0 ? 5 : image.getHeight() - 6))
                .isTrue();
        assertThat(new String(result, StandardCharsets.ISO_8859_1)).doesNotContain("Exif", "GPS-");
    }

    @Test
    void damagedMetadataDoesNotBreakTheUpload() throws IOException {
        byte[] photo = withExif(marked(40, 40, "jpg"), 6);
        // Desplazamiento del directorio fuera del archivo
        ByteBuffer.wrap(photo).putInt(16, 999_999);

        assertThat(read(ProductPhotos.normalize(photo)).getWidth()).isEqualTo(40);
    }
}
