package com.barboya.businesses;

import com.barboya.common.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpeningHoursTest {

    private static OpeningHours shift(int day, String opens, String closes) {
        return new OpeningHours(day, LocalTime.parse(opens), LocalTime.parse(closes));
    }

    /** El 2026-11-30 es lunes. */
    private static LocalDateTime monday(String time) {
        return LocalDateTime.parse("2026-11-30T" + time);
    }

    @Test
    void withoutShiftsTheScheduleDoesNotLimit() {
        assertThat(OpeningHours.covers(List.of(), monday("03:00"))).isTrue();
    }

    @Test
    void aShiftIncludesItsOpeningAndExcludesItsClosing() {
        var week = List.of(shift(1, "08:00", "12:00"), shift(1, "14:00", "20:00"));

        assertThat(OpeningHours.covers(week, monday("07:59"))).isFalse();
        assertThat(OpeningHours.covers(week, monday("08:00"))).isTrue();
        assertThat(OpeningHours.covers(week, monday("12:00"))).isFalse();
        assertThat(OpeningHours.covers(week, monday("19:59"))).isTrue();
        assertThat(OpeningHours.covers(week, monday("20:00"))).isFalse();
        // Martes sin turnos
        assertThat(OpeningHours.covers(week, monday("09:00").plusDays(1))).isFalse();
    }

    @Test
    void aShiftCanEndAfterMidnightEvenFromSundayToMonday() {
        var week = List.of(shift(7, "18:00", "02:00"));

        assertThat(OpeningHours.covers(week, monday("01:59"))).isTrue();
        assertThat(OpeningHours.covers(week, monday("02:00"))).isFalse();
        assertThat(OpeningHours.covers(week, monday("23:00").minusDays(1))).isTrue();
        assertThat(OpeningHours.covers(week, monday("01:00").minusDays(1))).isFalse();
        assertThat(OpeningHours.covers(week, monday("19:00"))).isFalse();
    }

    @Test
    void shiftsCannotBeEmptyOverlapOrBeMoreThanTwo() {
        for (var week : List.of(
                List.of(shift(1, "08:00", "08:00")),
                List.of(shift(1, "08:00", "14:00"), shift(1, "13:00", "20:00")),
                List.of(shift(1, "18:00", "02:00"), shift(1, "20:00", "23:00")),
                List.of(shift(1, "06:00", "08:00"), shift(1, "09:00", "10:00"), shift(1, "11:00", "12:00")))) {
            assertThatThrownBy(() -> OpeningHours.validate(week)).isInstanceOf(ApiException.class);
        }
        OpeningHours.validate(List.of(shift(1, "12:00", "15:00"), shift(1, "15:00", "01:00"),
                shift(2, "12:00", "15:00")));
    }
}
