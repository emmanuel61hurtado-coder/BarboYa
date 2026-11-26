package com.barboya.common;

import java.util.Locale;

/** Normaliza correo y celular para que un mismo contacto no se registre dos veces. */
public final class Contacts {

    private Contacts() {
    }

    public static String email(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /** Quita espacios y guiones; un celular colombiano de 10 digitos recibe +57. */
    public static String phone(String raw) {
        String value = raw.replaceAll("[\\s()-]", "");
        if (value.matches("3\\d{9}")) {
            return "+57" + value;
        }
        return value.startsWith("+") ? value : "+" + value;
    }
}
