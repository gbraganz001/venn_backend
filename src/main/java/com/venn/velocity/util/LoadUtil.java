package com.venn.velocity.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

import static java.time.DayOfWeek.MONDAY;

public final class LoadUtil {
    private LoadUtil() {}

    public static long parseCents(String loadAmount) {
        // "$123.45" -> 12345
        String normalized = loadAmount.replace("$", "").trim();
        BigDecimal bd = new BigDecimal(normalized);
        return bd.movePointRight(2).longValueExact();
    }

    public static LocalDate utcDay(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    public static LocalDate utcWeekStart(Instant instant) {
        LocalDate d = utcDay(instant);
        return d.with(TemporalAdjusters.previousOrSame(MONDAY));
    }
}
