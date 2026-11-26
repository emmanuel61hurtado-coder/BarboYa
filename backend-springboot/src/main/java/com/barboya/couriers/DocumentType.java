package com.barboya.couriers;

import java.util.EnumSet;
import java.util.Set;

/**
 * Documentos que se revisan. La lista exigida a cada vehiculo es una politica de
 * BarboYa que debe confirmar un abogado antes de operar.
 */
public enum DocumentType {
    IDENTITY("Documento de identidad", false),
    FACE_PHOTO("Foto del rostro", false),
    DRIVER_LICENSE("Licencia de conducción", true),
    SOAT("SOAT", true),
    TECHNICAL_REVIEW("Revisión técnico-mecánica", true);

    private final String label;
    private final boolean expires;

    DocumentType(String label, boolean expires) {
        this.label = label;
        this.expires = expires;
    }

    public String label() {
        return label;
    }

    public boolean expires() {
        return expires;
    }

    public static Set<DocumentType> requiredFor(VehicleType vehicle) {
        return vehicle == VehicleType.BICYCLE
                ? EnumSet.of(IDENTITY, FACE_PHOTO)
                : EnumSet.of(IDENTITY, FACE_PHOTO, DRIVER_LICENSE, SOAT);
    }

    /** La tecnico-mecanica solo es exigible a algunos vehiculos: la decide quien revisa. */
    public static Set<DocumentType> acceptedFor(VehicleType vehicle) {
        return vehicle == VehicleType.BICYCLE ? requiredFor(vehicle) : EnumSet.allOf(DocumentType.class);
    }
}
