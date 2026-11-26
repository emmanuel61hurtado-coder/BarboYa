package com.barboya.common;

import java.util.Locale;

/** Normaliza correo y celular para que un mismo contacto no se registre dos veces. */
public final class Contacts {

    private Contacts() {
    }

    public static String email(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Quita espacios y guiones. Los numeros colombianos de 10 digitos, celulares
     * (3...) o fijos (60...), reciben el indicativo +57.
     */
    public static String phone(String raw) {
        String value = raw.replaceAll("[\\s()-]", "");
        if (value.matches("(3|60)\\d+") && value.length() == 10) {
            return "+57" + value;
        }
        return value.startsWith("+") ? value : "+" + value;
    }
}
