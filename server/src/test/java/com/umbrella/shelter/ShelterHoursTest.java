package com.umbrella.shelter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ShelterHoursTest {

    private static Shelter shelter(String open, String close, boolean weekendOpen) {
        return new Shelter("1", "경로당", 37.5, 127.0, "주소", "경로당", open, close, weekendOpen, false, true);
    }

    // 2026-07-22는 수요일, 2026-07-25는 토요일
    private static final LocalDateTime WEDNESDAY_2PM = LocalDateTime.of(2026, 7, 22, 14, 0);
    private static final LocalDateTime WEDNESDAY_8PM = LocalDateTime.of(2026, 7, 22, 20, 0);
    private static final LocalDateTime SATURDAY_2PM = LocalDateTime.of(2026, 7, 25, 14, 0);

    @Test
    void 운영시간은_여러_형식을_읽는다() {
        assertThat(ShelterHours.parse("0900")).contains(LocalTime.of(9, 0));
        assertThat(ShelterHours.parse("09:00")).contains(LocalTime.of(9, 0));
        assertThat(ShelterHours.parse("9:30")).contains(LocalTime.of(9, 30));
        assertThat(ShelterHours.parse("2400")).contains(LocalTime.MAX);
        assertThat(ShelterHours.parse("상시")).isEmpty();
    }

    @Test
    void 평일_운영시간_안이면_운영_중이고_밖이면_아니다() {
        assertThat(ShelterHours.openNow(shelter("0900", "1800", false), WEDNESDAY_2PM)).isTrue();
        assertThat(ShelterHours.openNow(shelter("0900", "1800", false), WEDNESDAY_8PM)).isFalse();
    }

    @Test
    void 주말에는_주말_개방인_곳만_운영_중으로_본다() {
        assertThat(ShelterHours.openNow(shelter("0900", "1800", false), SATURDAY_2PM)).isFalse();
        assertThat(ShelterHours.openNow(shelter("0900", "1800", true), SATURDAY_2PM)).isTrue();
    }

    @Test
    void 운영시간을_모르면_판단하지_않는다() {
        assertThat(ShelterHours.openNow(shelter(null, null, false), WEDNESDAY_2PM)).isNull();
    }

    @Test
    void 운영시간을_읽기_쉬운_한_줄로_만든다() {
        assertThat(ShelterHours.describe(shelter("0900", "1800", true))).isEqualTo("평일 09:00~18:00 · 주말 개방 · 냉방기");
    }
}
