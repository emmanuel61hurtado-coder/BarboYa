package com.barboya.businesses;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;

/**
 * Guarda una hora local como minutos desde la medianoche. Una columna TIME se
 * desplazaria segun la zona horaria del servidor.
 */
@Converter
public class MinuteOfDay implements AttributeConverter<LocalTime, Integer> {

    @Override
    public Integer convertToDatabaseColumn(LocalTime time) {
        return time == null ? null : time.getHour() * 60 + time.getMinute();
    }

    @Override
    public LocalTime convertToEntityAttribute(Integer minutes) {
        return minutes == null ? null : LocalTime.ofSecondOfDay(minutes * 60L);
    }
}
