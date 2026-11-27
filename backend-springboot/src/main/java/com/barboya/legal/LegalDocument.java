package com.barboya.legal;

/**
 * Documentos que la persona debe aceptar. <strong>Si cambia el texto de un
 * archivo, hay que cambiar aqui su version</strong>: asi se vuelve a pedir la
 * aceptacion y queda claro que aprobo cada quien.
 */
public enum LegalDocument {
    TERMS("terminos", "2026-11-borrador-1"),
    PRIVACY("privacidad", "2026-11-borrador-1");

    private final String slug;
    private final String version;

    LegalDocument(String slug, String version) {
        this.slug = slug;
        this.version = version;
    }

    /** Nombre del archivo y de la direccion publica. */
    public String slug() {
        return slug;
    }

    public String version() {
        return version;
    }
}
