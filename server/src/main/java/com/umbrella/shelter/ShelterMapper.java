package com.umbrella.shelter;

import java.util.Map;
import java.util.Optional;

// 공공데이터 원본 필드 → Shelter. 필드 이름이 바뀌면 여기만 고치면 된다
final class ShelterMapper {

    static final String ID = "RSTR_FCLTY_NO";
    static final String NAME = "RSTR_NM";
    static final String LAT = "LA";
    static final String LON = "LO";
    static final String ROAD_ADDRESS = "RN_DTL_ADRES";
    static final String ADDRESS = "DTL_ADRES";
    static final String TYPE = "FCLTY_TY";
    static final String WEEKDAY_OPEN = "WKDAY_OPER_BEGIN_TIME";
    static final String WEEKDAY_CLOSE = "WKDAY_OPER_END_TIME";
    static final String WEEKEND_OPEN = "CHCK_MATTER_WKEND_HDAY_OPN_AT";
    static final String NIGHT_OPEN = "CHCK_MATTER_NIGHT_OPN_AT";
    static final String AIR_CONDITIONER = "COLR_HOLD_ARCNDTN";

    private ShelterMapper() {
    }

    // 이름이나 좌표가 없으면 지도에서 찾아갈 수 없으니 버린다
    static Optional<Shelter> map(Map<String, Object> raw) {
        String name = text(raw, NAME);
        Double lat = number(raw, LAT);
        Double lon = number(raw, LON);
        if (name == null || lat == null || lon == null || !inKorea(lat, lon)) {
            return Optional.empty();
        }
        String road = text(raw, ROAD_ADDRESS);
        Double airConditioners = number(raw, AIR_CONDITIONER);
        return Optional.of(new Shelter(
                text(raw, ID),
                name,
                lat,
                lon,
                road != null ? road : text(raw, ADDRESS),
                text(raw, TYPE),
                text(raw, WEEKDAY_OPEN),
                text(raw, WEEKDAY_CLOSE),
                yes(raw, WEEKEND_OPEN),
                yes(raw, NIGHT_OPEN),
                airConditioners != null && airConditioners > 0));
    }

    private static boolean inKorea(double lat, double lon) {
        return lat >= 33 && lat <= 39 && lon >= 124 && lon <= 132;
    }

    private static String text(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static Double number(Map<String, Object> raw, String key) {
        String text = text(raw, key);
        if (text == null) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean yes(Map<String, Object> raw, String key) {
        String text = text(raw, key);
        return text != null && (text.equalsIgnoreCase("Y") || text.equals("1") || text.equals("예"));
    }
}
