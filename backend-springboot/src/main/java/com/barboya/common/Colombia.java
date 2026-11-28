package com.barboya.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Las fechas y horas que ve la gente son las de Colombia, esten donde esten los servidores. */
public final class Colombia {

    public static final ZoneId ZONE = ZoneId.of("America/Bogota");

    private Colombia() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.ofInstant(clock.instant(), ZONE);
    }
}
