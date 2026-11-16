package com.umbrella.common;

// 좌표를 격자로 묶어 캐시 키로 쓴다. 바로 옆 사람끼리는 같은 결과를 나눠 써서 외부 API 호출을 줄인다
public record GeoGrid(double lat, double lon) {

    public static GeoGrid of(double lat, double lon, double cellDegrees) {
        return new GeoGrid(snap(lat, cellDegrees), snap(lon, cellDegrees));
    }

    private static double snap(double value, double cell) {
        // 부동소수점 꼬리를 없애려고 소수 5자리에서 자른다
        return Math.round(Math.round(value / cell) * cell * 100_000) / 100_000.0;
    }

    public String key() {
        return lat + "," + lon;
    }
}
