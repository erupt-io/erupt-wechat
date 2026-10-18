package xyz.erupt.wechat.live;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The date range a statistics page asks for: the searched range, capped, else the trailing default.
 *
 * @author YuePeng
 */
final class WxStats {

    private WxStats() {
    }

    // WeChat reports a day the morning after, so the trailing window ends yesterday
    static LocalDate[] dateRange(Object condition, int maxDays, int defaultDays) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate from = yesterday.minusDays(defaultDays - 1), to = yesterday;
        if (condition instanceof List<?> range && range.size() == 2) {
            from = parse(range.get(0));
            to = parse(range.get(1));
        }
        if (to.isAfter(yesterday)) to = yesterday;
        if (from.isBefore(to.minusDays(maxDays - 1))) from = to.minusDays(maxDays - 1);
        return new LocalDate[]{from, to};
    }

    static LocalDateTime[] dateTimeRange(Object condition, int maxDays, LocalDateTime defaultFrom) {
        LocalDateTime from = defaultFrom, to = LocalDateTime.now();
        if (condition instanceof List<?> range && range.size() == 2) {
            from = parseTime(range.get(0));
            to = parseTime(range.get(1));
        }
        if (from.isBefore(to.minusDays(maxDays))) from = to.minusDays(maxDays);
        return new LocalDateTime[]{from, to};
    }

    private static LocalDate parse(Object value) {
        return LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    private static LocalDateTime parseTime(Object value) {
        String text = String.valueOf(value);
        return text.length() > 10 ? LocalDateTime.parse(text.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : LocalDate.parse(text).atStartOfDay();
    }

}
