package com.umbrella.spot;

import java.util.List;

// stale = 세 서버가 모두 실패해서, 전에 받아 둔 결과를 대신 보여 주는 중
public record SpotResponse(String category, List<Spot> spots, boolean stale) {

    public record Spot(String name, String category, int distanceMeters, int walkMinutes) {
    }
}
