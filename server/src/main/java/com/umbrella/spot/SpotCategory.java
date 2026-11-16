package com.umbrella.spot;

import java.util.List;
import java.util.Map;

public enum SpotCategory {

    // 갑자기 비가 올 때 바로 들어갈 수 있는 곳. 걸어서 15분 안쪽
    RAIN(1000, List.of("cafe", "restaurant")),
    // 더위를 식힐 수 있는 공공장소
    HEAT(1500, List.of("community_centre", "library", "townhall"));

    private static final Map<String, String> LABELS = Map.of(
            "cafe", "카페",
            "restaurant", "음식점",
            "library", "도서관",
            "townhall", "주민센터",
            "community_centre", "주민센터");

    private final int radiusMeters;
    private final List<String> amenities;

    SpotCategory(int radiusMeters, List<String> amenities) {
        this.radiusMeters = radiusMeters;
        this.amenities = amenities;
    }

    public int radiusMeters() {
        return radiusMeters;
    }

    String overpassQuery(double lat, double lon) {
        StringBuilder nodes = new StringBuilder();
        for (String amenity : amenities) {
            nodes.append("node[\"amenity\"=\"").append(amenity).append("\"](around:")
                    .append(radiusMeters).append(',').append(lat).append(',').append(lon).append(");");
        }
        return "[out:json][timeout:8];(" + nodes + ");out body 40;";
    }

    static String label(String amenity) {
        return LABELS.getOrDefault(amenity, "쉼터");
    }
}
