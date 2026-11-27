package com.barboya.files;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Lee unicamente la orientacion (1 a 8) de los metadatos EXIF de un JPEG. */
final class ExifOrientation {

    private static final int ORIENTATION_TAG = 0x0112;
    private static final int NORMAL = 1;

    private ExifOrientation() {
    }

    static int of(byte[] file) {
        try {
            ByteBuffer data = ByteBuffer.wrap(file);
            if ((data.getShort() & 0xFFFF) != 0xFFD8) {
                return NORMAL;
            }
            while (data.remaining() >= 4) {
                int marker = data.getShort() & 0xFFFF;
                int length = data.getShort() & 0xFFFF;
                // 0xFFDA inicia los pixeles: despues ya no hay metadatos
                if ((marker & 0xFF00) != 0xFF00 || marker == 0xFFDA || length < 2) {
                    return NORMAL;
                }
                int next = data.position() + length - 2;
                // "Exif" seguido de dos ceros
                if (marker == 0xFFE1 && length > 16 && data.getInt() == 0x45786966 && data.getShort() == 0) {
                    return read(data.slice());
                }
                data.position(next);
            }
        } catch (RuntimeException ex) {
            // Metadatos mal formados: se deja la imagen como esta
        }
        return NORMAL;
    }

    private static int read(ByteBuffer tiff) {
        tiff.order(tiff.getShort(0) == 0x4949 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        int directory = tiff.getInt(4);
        int entries = tiff.getShort(directory) & 0xFFFF;
        for (int i = 0; i < entries; i++) {
            int entry = directory + 2 + i * 12;
            if ((tiff.getShort(entry) & 0xFFFF) == ORIENTATION_TAG) {
                int value = tiff.getShort(entry + 8) & 0xFFFF;
                return value >= 1 && value <= 8 ? value : NORMAL;
            }
        }
        return NORMAL;
    }
}
