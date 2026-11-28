package com.barboya.files;

/** Almacen privado de archivos. Un almacen compatible con S3 implementaria esta interfaz. */
public interface FileStorage {

    /** @return la clave con la que se recupera el archivo */
    String store(byte[] content);

    byte[] read(String key);

    void delete(String key);
}
