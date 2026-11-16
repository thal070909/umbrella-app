package com.umbrella.shelter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 운영시간 문자열을 사람이 읽는 문구와 "지금 운영 중" 판단으로 바꾼다
// 원본 형식이 "0900", "09:00", "9:00" 등으로 섞여 있어서 숫자만 뽑아 해석하고, 못 읽으면 판단하지 않는다
public final class ShelterHours {

    private ShelterHours() {
    }

    static Optional<LocalTime> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 3) {
            digits = "0" + digits;
        }
        if (digits.length() != 4) {
            return Optional.empty();
        }
        int hour = Integer.parseInt(digits.substring(0, 2));
        int minute = Integer.parseInt(digits.substring(2));
        if (hour == 24 && minute == 0) {
            return Optional.of(LocalTime.MAX);
        }
        if (hour > 23 || minute > 59) {
            return Optional.empty();
        }
        return Optional.of(LocalTime.of(hour, minute));
    }

    // true/false = 운영 중/아님, null = 운영시간을 몰라서 판단하지 않음
    // 주말·공휴일은 '주말 개방'인 곳만 평일 시간과 같다고 보고, 공휴일은 알 수 없어 평일처럼 본다
    public static Boolean openNow(Shelter shelter, LocalDateTime now) {
        boolean weekend = now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (weekend && !shelter.weekendOpen()) {
            return false;
        }
        Optional<LocalTime> open = parse(shelter.weekdayOpen());
        Optional<LocalTime> close = parse(shelter.weekdayClose());
        if (open.isEmpty() || close.isEmpty()) {
            return null;
        }
        LocalTime time = now.toLocalTime();
        return !time.isBefore(open.get()) && !time.isAfter(close.get());
    }

    // "평일 09:00~18:00 · 주말 개방 · 냉방기"
    public static String describe(Shelter shelter) {
        List<String> parts = new ArrayList<>();
        Optional<LocalTime> open = parse(shelter.weekdayOpen());
        Optional<LocalTime> close = parse(shelter.weekdayClose());
        if (open.isPresent() && close.isPresent()) {
            parts.add("평일 " + format(open.get()) + "~" + format(close.get()));
        }
        if (shelter.weekendOpen()) {
            parts.add("주말 개방");
        }
        if (shelter.nightOpen()) {
            parts.add("야간 개방");
        }
        if (shelter.airConditioner()) {
            parts.add("냉방기");
        }
        return parts.isEmpty() ? shelter.address() : String.join(" · ", parts);
    }

    private static String format(LocalTime time) {
        return time.equals(LocalTime.MAX) ? "24:00" : "%02d:%02d".formatted(time.getHour(), time.getMinute());
    }
}
