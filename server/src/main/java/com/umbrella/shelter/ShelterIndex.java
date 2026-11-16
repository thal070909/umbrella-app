package com.umbrella.shelter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 전국 쉼터(약 6만 곳)를 약 1km 칸으로 나눠 둔 메모리 색인. 근처 칸만 보면 되니 매번 6만 곳을 다 훑지 않는다
public final class ShelterIndex {

    static final double CELL_DEGREES = 0.01;
    // 0.01도는 위도로 약 1.1km, 우리나라 경도로 약 0.88km. 짧은 쪽 기준으로 몇 칸을 볼지 정한다
    private static final double CELL_METERS = 880;

    private final Map<Long, List<Shelter>> cells = new HashMap<>();
    private final int size;

    public ShelterIndex(List<Shelter> shelters) {
        for (Shelter shelter : shelters) {
            cells.computeIfAbsent(cellKey(cell(shelter.lat()), cell(shelter.lon())), k -> new ArrayList<>()).add(shelter);
        }
        this.size = shelters.size();
    }

    public int size() {
        return size;
    }

    public List<Nearby> nearest(double lat, double lon, int radiusMeters, int limit) {
        int range = (int) Math.ceil(radiusMeters / CELL_METERS);
        long baseLat = cell(lat);
        long baseLon = cell(lon);
        List<Nearby> found = new ArrayList<>();
        for (long dLat = -range; dLat <= range; dLat++) {
            for (long dLon = -range; dLon <= range; dLon++) {
                for (Shelter shelter : cells.getOrDefault(cellKey(baseLat + dLat, baseLon + dLon), List.of())) {
                    int meters = (int) Math.round(haversineMeters(lat, lon, shelter.lat(), shelter.lon()));
                    if (meters <= radiusMeters) {
                        found.add(new Nearby(shelter, meters));
                    }
                }
            }
        }
        return found.stream().sorted(Comparator.comparingInt(Nearby::distanceMeters)).limit(limit).toList();
    }

    public record Nearby(Shelter shelter, int distanceMeters) {
    }

    private static long cell(double degrees) {
        return (long) Math.floor(degrees / CELL_DEGREES);
    }

    private static long cellKey(long latCell, long lonCell) {
        return latCell * 1_000_000L + lonCell;
    }

    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
