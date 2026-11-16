package com.umbrella.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.umbrella.weather.OpenMeteoClient.Current;
import com.umbrella.weather.OpenMeteoClient.Hourly;
import com.umbrella.weather.WeatherAdvice.Umbrella;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeatherAdviceTest {

    // 2026-07-23 00시부터 24시간치 시간별 값. rainHour 시각만 강수확률 70%
    private static Hourly hourlyWithRainAt(int rainHour) {
        List<String> times = new ArrayList<>();
        List<Integer> probabilities = new ArrayList<>();
        List<Double> amounts = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            times.add("2026-07-23T%02d:00".formatted(h));
            probabilities.add(h == rainHour ? 70 : 10);
            amounts.add(0.0);
        }
        return new Hourly(times, probabilities, amounts);
    }

    private static Current clearAt(String time) {
        return new Current(time, 27, 29, 0, 0, 60);
    }

    @Test
    void 지금_비가_오면_바로_우산을_챙기라고_한다() {
        Current raining = new Current("2026-07-23T09:20", 24, 25, 1.2, 61, 90);

        Umbrella umbrella = WeatherAdvice.umbrella(raining, hourlyWithRainAt(-1));

        assertThat(umbrella.needed()).isTrue();
        assertThat(umbrella.message()).isEqualTo("지금 비가 오고 있어요, 우산을 챙기세요");
    }

    @Test
    void 지금은_맑아도_오후에_비_소식이_있으면_몇_시부터인지_알려준다() {
        Umbrella umbrella = WeatherAdvice.umbrella(clearAt("2026-07-23T09:20"), hourlyWithRainAt(15));

        assertThat(umbrella.needed()).isTrue();
        assertThat(umbrella.rainStartsAt()).isEqualTo("2026-07-23T15:00");
        assertThat(umbrella.message()).isEqualTo("오후 3시부터 비 소식이 있어요, 우산을 챙기세요");
    }

    @Test
    void 이미_지난_시각의_비_소식은_보지_않는다() {
        Umbrella umbrella = WeatherAdvice.umbrella(clearAt("2026-07-23T16:05"), hourlyWithRainAt(15));

        assertThat(umbrella.needed()).isFalse();
    }

    @Test
    void 열두_시간보다_뒤의_비_소식은_오늘_외출과_상관없어서_알리지_않는다() {
        Umbrella umbrella = WeatherAdvice.umbrella(clearAt("2026-07-23T06:00"), hourlyWithRainAt(19));

        assertThat(umbrella.needed()).isFalse();
    }

    @Test
    void 옷차림은_실제_기온이_아니라_체감온도_구간으로_고른다() {
        assertThat(WeatherAdvice.outfit(28).title()).isEqualTo("민소매 · 반바지");
        assertThat(WeatherAdvice.outfit(27).title()).isEqualTo("반팔 · 얇은 셔츠");
        assertThat(WeatherAdvice.outfit(4).title()).isEqualTo("패딩 · 목도리 · 장갑");
    }

    @Test
    void 기온이_30도를_넘으면_차_안_물건_주의를_켠다() {
        assertThat(WeatherAdvice.heatCarAlert(30)).isTrue();
        assertThat(WeatherAdvice.heatCarAlert(29)).isFalse();
    }

    @Test
    void 시각은_오전_오후로_읽기_쉽게_바꾼다() {
        assertThat(WeatherAdvice.hourLabel("2026-07-23T09:00")).isEqualTo("오전 9시");
        assertThat(WeatherAdvice.hourLabel("2026-07-23T12:00")).isEqualTo("낮 12시");
        assertThat(WeatherAdvice.hourLabel("2026-07-23T18:00")).isEqualTo("오후 6시");
    }
}
