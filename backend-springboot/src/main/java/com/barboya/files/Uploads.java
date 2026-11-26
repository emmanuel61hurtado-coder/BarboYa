package com.barboya.files;

import com.barboya.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/** Valida archivos recibidos por su contenido; el nombre y el tipo declarados no cuentan. */
public final class Uploads {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 40_000_000;
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};

    public record Upload(byte[] content, String contentType) {
    }

    private Uploads() {
    }

    public static Upload read(MultipartFile file, boolean allowPdf) {
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw invalid("El archivo debe pesar entre 1 byte y 5 MB");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw invalid("No se pudo leer el archivo");
        }
        if (startsWith(content, JPEG)) {
            requireImage(content);
            return new Upload(content, "image/jpeg");
        }
        if (startsWith(content, PNG)) {
            requireImage(content);
            return new Upload(content, "image/png");
        }
        if (allowPdf && startsWith(content, PDF)) {
            return new Upload(content, "application/pdf");
        }
        throw invalid(allowPdf ? "Sube una imagen JPG o PNG, o un PDF" : "Sube una imagen JPG o PNG");
    }

    /** Lee solo la cabecera: comprueba que sea una imagen y que no sea desmesurada. */
    private static void requireImage(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalid("La imagen esta dañada");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw invalid("La imagen tiene demasiados pixeles");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException | IndexOutOfBoundsException ex) {
            throw invalid("La imagen esta dañada");
        }
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        return content.length > signature.length
                && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE", message);
    }
}
