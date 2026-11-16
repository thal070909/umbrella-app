package com.umbrella.weather;

import com.umbrella.weather.OpenMeteoClient.Current;
import com.umbrella.weather.OpenMeteoClient.Hourly;
import java.util.Optional;
import java.util.Set;

// 날씨 숫자를 사용자가 바로 할 일(우산, 옷차림, 차 안 물건)로 바꾸는 규칙. 외부 호출 없이 계산만 한다
public final class WeatherAdvice {

    // 강수확률이 이 값 이상이거나 비가 조금이라도 오면 "비 소식"으로 본다
    static final int RAIN_PROBABILITY = 50;
    static final double RAIN_MM = 0.1;
    // 지금부터 몇 시간 뒤까지 비 소식을 찾을지 (외출했다 돌아올 시간 정도)
    static final int LOOKAHEAD_HOURS = 12;
    static final int HEAT_CAR_TEMPERATURE = 30;

    private static final Set<Integer> RAIN_CODES = Set.of(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82);
    private static final Set<Integer> THUNDER_CODES = Set.of(95, 96, 99);
    private static final Set<Integer> SNOW_CODES = Set.of(71, 73, 75, 77, 85, 86);
    private static final Set<Integer> FOG_CODES = Set.of(45, 48);

    private WeatherAdvice() {
    }

    public record Condition(String key, String text) {
    }

    public record Umbrella(boolean needed, String message, String rainStartsAt) {
    }

    public record Outfit(String title, String description) {
    }

    static Condition condition(int code) {
        if (code == 0) {
            return new Condition("clear", "맑음");
        }
        if (code >= 1 && code <= 3) {
            return new Condition("cloudy", "구름 많음");
        }
        if (FOG_CODES.contains(code)) {
            return new Condition("fog", "안개");
        }
        if (RAIN_CODES.contains(code)) {
            return new Condition("rain", "비");
        }
        if (SNOW_CODES.contains(code)) {
            return new Condition("snow", "눈");
        }
        if (THUNDER_CODES.contains(code)) {
            return new Condition("thunder", "뇌우");
        }
        return new Condition("cloudy", "흐림");
    }

    // 기온이 아니라 체감온도로 고른다. 같은 25도라도 습하면 더 덥게 느껴져서
    static Outfit outfit(int feelsLike) {
        if (feelsLike >= 28) return new Outfit("민소매 · 반바지", "가볍고 통풍 잘 되는 옷차림이 좋아요. 자외선 차단제도 챙기세요.");
        if (feelsLike >= 23) return new Outfit("반팔 · 얇은 셔츠", "반팔에 얇은 셔츠나 원피스면 충분히 시원해요.");
        if (feelsLike >= 20) return new Outfit("얇은 가디건 · 긴바지", "낮과 밤의 온도차가 있으니 얇은 겉옷을 챙기세요.");
        if (feelsLike >= 17) return new Outfit("가디건 · 자켓", "아침저녁으로 쌀쌀할 수 있어요, 가벼운 자켓을 추천해요.");
        if (feelsLike >= 12) return new Outfit("자켓 · 니트", "니트에 자켓을 더하면 딱 좋은 날씨예요.");
        if (feelsLike >= 9) return new Outfit("코트 · 니트", "코트와 니트로 보온에 신경 써주세요.");
        if (feelsLike >= 5) return new Outfit("두꺼운 코트 · 목도리", "히트텍과 목도리로 든든하게 챙겨 입으세요.");
        return new Outfit("패딩 · 목도리 · 장갑", "매우 추운 날씨예요, 패딩과 장갑까지 완전 무장하세요.");
    }

    // 지금 비가 오면 바로, 아니면 앞으로 12시간 안에 처음 비 소식이 있는 시각을 알려 준다
    // "강수확률 60%" 대신 "오후 3시부터 비"라고 말해야 우산을 챙길지 바로 정할 수 있어서
    static Umbrella umbrella(Current current, Hourly hourly) {
        Condition condition = condition(current.weatherCode());
        boolean rainingNow = current.precipitation() > 0
                || "rain".equals(condition.key()) || "thunder".equals(condition.key());
        if (rainingNow) {
            return new Umbrella(true, "지금 비가 오고 있어요, 우산을 챙기세요", null);
        }
        return firstRainHour(current.time(), hourly)
                .map(hour -> new Umbrella(true, hourLabel(hour) + "부터 비 소식이 있어요, 우산을 챙기세요", hour))
                .orElse(new Umbrella(false, null, null));
    }

    static Optional<String> firstRainHour(String currentTime, Hourly hourly) {
        if (hourly == null || hourly.time() == null || currentTime == null || currentTime.length() < 13) {
            return Optional.empty();
        }
        String currentHour = currentTime.substring(0, 13) + ":00";
        int start = hourly.time().indexOf(currentHour);
        if (start < 0) {
            return Optional.empty();
        }
        int end = Math.min(hourly.time().size(), start + LOOKAHEAD_HOURS + 1);
        for (int i = start; i < end; i++) {
            Integer probability = valueAt(hourly.precipitationProbability(), i);
            Double amount = valueAt(hourly.precipitation(), i);
            if ((probability != null && probability >= RAIN_PROBABILITY) || (amount != null && amount >= RAIN_MM)) {
                return Optional.of(hourly.time().get(i));
            }
        }
        return Optional.empty();
    }

    static int probabilityNow(String currentTime, Hourly hourly) {
        if (hourly == null || hourly.time() == null || currentTime == null || currentTime.length() < 13) {
            return 0;
        }
        int index = hourly.time().indexOf(currentTime.substring(0, 13) + ":00");
        Integer probability = index < 0 ? null : valueAt(hourly.precipitationProbability(), index);
        return probability == null ? 0 : probability;
    }

    static boolean heatCarAlert(int temperature) {
        return temperature >= HEAT_CAR_TEMPERATURE;
    }

    static String message(Condition condition, int temperature) {
        return switch (condition.key()) {
            case "rain" -> "우산을 챙기고 물웅덩이 조심하세요";
            case "thunder" -> "천둥번개가 있어요, 외출을 주의하세요";
            case "snow" -> "눈길 미끄럼에 주의하세요";
            case "clear" -> temperature >= 28 ? "더위에 수분 보충 잊지 마세요"
                    : temperature <= 5 ? "건조하고 쌀쌀하니 보습 챙기세요" : "오늘 하루도 좋은 하루 보내세요";
            default -> "오늘 하루도 좋은 하루 보내세요";
        };
    }

    // "2026-07-23T15:00" → "오후 3시"
    static String hourLabel(String isoHour) {
        int hour = Integer.parseInt(isoHour.substring(11, 13));
        if (hour == 0) return "자정";
        if (hour == 12) return "낮 12시";
        return hour < 12 ? "오전 " + hour + "시" : "오후 " + (hour - 12) + "시";
    }

    private static <T> T valueAt(java.util.List<T> list, int index) {
        return list == null || index >= list.size() ? null : list.get(index);
    }
}
