package com.barboya.businesses;

import com.barboya.common.ApiException;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Un turno de atencion. {@code day}: 1 lunes a 7 domingo. Si {@code closes} es
 * menor que {@code opens}, el turno termina despues de medianoche.
 */
@Embeddable
public record OpeningHours(
        @NotNull @Min(1) @Max(7) Integer day,
        @NotNull @JsonFormat(pattern = "HH:mm") @Convert(converter = MinuteOfDay.class) LocalTime opens,
        @NotNull @JsonFormat(pattern = "HH:mm") @Convert(converter = MinuteOfDay.class) LocalTime closes) {

    static final Comparator<OpeningHours> ORDER =
            Comparator.comparing(OpeningHours::day).thenComparing(OpeningHours::opens);

    private boolean overnight() {
        return closes.isBefore(opens);
    }

    /** Sin turnos definidos el horario no limita: se considera abierto. */
    static boolean covers(Collection<OpeningHours> week, LocalDateTime moment) {
        if (week.isEmpty()) {
            return true;
        }
        int today = moment.getDayOfWeek().getValue();
        int yesterday = today == 1 ? 7 : today - 1;
        LocalTime time = moment.toLocalTime();
        return week.stream().anyMatch(shift -> {
            if (shift.day == today) {
                return !time.isBefore(shift.opens) && (shift.overnight() || time.isBefore(shift.closes));
            }
            return shift.day == yesterday && shift.overnight() && time.isBefore(shift.closes);
        });
    }

    /** Maximo dos turnos por dia, sin cruzarse; solo el ultimo puede pasar de medianoche. */
    static void validate(List<OpeningHours> week) {
        Map<Integer, List<OpeningHours>> days = week.stream().sorted(ORDER)
                .collect(Collectors.groupingBy(OpeningHours::day));
        for (List<OpeningHours> shifts : days.values()) {
            if (shifts.size() > 2) {
                throw invalid("Cada dia admite maximo dos turnos");
            }
            for (OpeningHours shift : shifts) {
                if (shift.opens.equals(shift.closes)) {
                    throw invalid("La hora de cierre debe ser distinta a la de apertura");
                }
            }
            if (shifts.size() == 2 && (shifts.get(0).overnight()
                    || shifts.get(1).opens.isBefore(shifts.get(0).closes))) {
                throw invalid("Los turnos de un mismo dia no se pueden cruzar");
            }
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_HOURS", message);
    }
}
