package com.badminton.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class DateTimeUtils {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private static final List<DateTimeFormatter> DATE_TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private DateTimeUtils() {
    }

    public static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException("时间不能为空");
        }
        String raw = value.trim();

        // ISO 8601 带时区，例如 2026-11-20T14:00:00+08:00 或 2026-11-20T06:00:00Z
        try {
            return OffsetDateTime.parse(raw)
                    .atZoneSameInstant(BUSINESS_ZONE)
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // 继续尝试本地时间格式
        }

        String text = raw.replace('T', ' ');
        for (DateTimeFormatter formatter : DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        try {
            return LocalDate.parse(text).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        throw new BizException("时间格式不正确，应为 ISO 8601 或 yyyy-MM-dd HH:mm:ss");
    }

    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BizException("日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    public static String format(LocalDateTime time) {
        return time == null ? null : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
