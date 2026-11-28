package com.barboya.common;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Plazos en dias habiles. Solo descuenta sabados y domingos: al no descontar
 * festivos la fecha sale igual o antes que la legal, nunca despues.
 */
public final class BusinessDays {

    private BusinessDays() {
    }

    /** Final (hora de Colombia) del dia habil numero {@code days} despues de {@code start}. */
    public static Instant deadline(LocalDate start, int days) {
        LocalDate date = start;
        for (int counted = 0; counted < days; ) {
            date = date.plusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                counted++;
            }
        }
        return date.atTime(LocalTime.of(23, 59, 59)).atZone(Colombia.ZONE).toInstant();
    }
}
