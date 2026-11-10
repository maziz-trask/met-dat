package cz.metdat.service;

import cz.metdat.exception.ValidationException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ISO-8601 calendar-week arithmetic. A week runs Monday-Sunday; week 1 is the week containing 4 January.
 * A test given only as a calendar week (workbook format KTww/yy) occupies the whole Monday-Sunday period.
 */
public final class IsoWeeks {
    private IsoWeeks() {}
    private static final Pattern KT = Pattern.compile("^KT(\\d{2})/(\\d{2})$");

    public record Period(int year, int week, LocalDate exactDate, LocalDate start, LocalDate end) {}

    public static Period ofWeek(int year, int week) {
        int max = (int) LocalDate.of(year, 12, 28).range(IsoFields.WEEK_OF_WEEK_BASED_YEAR).getMaximum();
        if (week < 1 || week > max) throw new ValidationException("testWeek", "week.range", String.valueOf(year), String.valueOf(max));
        LocalDate monday = LocalDate.of(year, 1, 4).with(DayOfWeek.MONDAY).plusWeeks(week - 1L);
        return new Period(year, week, null, monday, monday.plusDays(6));
    }

    public static Period ofLabel(String label) {
        Matcher m = KT.matcher(label == null ? "" : label.trim());
        if (!m.matches()) throw new ValidationException("testWeek", "validation.week.format");
        return ofWeek(2000 + Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
    }

    public static Period ofDate(LocalDate d) {
        return new Period(d.get(IsoFields.WEEK_BASED_YEAR), d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), d, d, d);
    }
}
