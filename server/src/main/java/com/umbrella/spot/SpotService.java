package com.umbrella.spot;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.umbrella.common.GeoGrid;
import com.umbrella.config.UmbrellaProperties;
import com.umbrella.spot.OverpassClient.Element;
import com.umbrella.spot.OverpassClient.OverpassResponse;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class SpotService {

    // 약 200m. 같은 블록에 있는 사람은 같은 검색 결과를 나눠 쓴다 (거리는 각자 위치로 다시 계산)
    static final double GRID_DEGREES = 0.002;
    static final int MAX_SPOTS = 15;
    // 사람 걷는 속도 분당 약 67m
    static final double WALK_METERS_PER_MINUTE = 67;

    private final OverpassClient overpassClient;
    private final ExecutorService spotExecutor;
    private final List<String> endpoints;
    private final long timeoutMillis;
    private final Cache<String, List<Element>> fresh;
    // 세 서버가 모두 실패할 때 꺼내 쓰는 예전 결과. fresh보다 오래 들고 있는다
    private final Cache<String, List<Element>> stale;

    public SpotService(OverpassClient overpassClient, ExecutorService spotExecutor, UmbrellaProperties properties) {
        this.overpassClient = overpassClient;
        this.spotExecutor = spotExecutor;
        this.endpoints = properties.spots().endpoints();
        this.timeoutMillis = properties.spots().timeout().toMillis();
        this.fresh = Caffeine.newBuilder().expireAfterWrite(properties.spots().cacheTtl()).maximumSize(20_000).build();
        this.stale = Caffeine.newBuilder().expireAfterWrite(properties.spots().staleTtl()).maximumSize(20_000).build();
    }

    // refresh = 사용자가 '다시 찾기'를 눌렀을 때. 캐시를 건너뛰고 새로 찾는다
    public SpotResponse spots(double lat, double lon, SpotCategory category, boolean refresh) {
        GeoGrid grid = GeoGrid.of(lat, lon, GRID_DEGREES);
        String key = category + ":" + grid.key();

        if (!refresh) {
            List<Element> cached = fresh.getIfPresent(key);
            if (cached != null) {
                return toResponse(category, cached, lat, lon, false);
            }
        }
        try {
            List<Element> elements = fetchFromFastestMirror(category.overpassQuery(grid.lat(), grid.lon()));
            fresh.put(key, elements);
            stale.put(key, elements);
            return toResponse(category, elements, lat, lon, false);
        } catch (SpotsUnavailableException e) {
            List<Element> previous = stale.getIfPresent(key);
            if (previous != null) {
                return toResponse(category, previous, lat, lon, true);
            }
            throw e;
        }
    }

    // 서버 세 곳에 동시에 보내고 가장 먼저 "성공"한 응답을 쓴다
    // 순서대로 시도하면 앞 서버가 느린 만큼 사용자가 기다려서 (최악 36초), 전체 대기도 timeout으로 묶는다
    List<Element> fetchFromFastestMirror(String query) {
        CompletableFuture<OverpassResponse> firstSuccess = new CompletableFuture<>();
        AtomicInteger failures = new AtomicInteger();
        List<CompletableFuture<OverpassResponse>> attempts = endpoints.stream()
                .map(endpoint -> CompletableFuture.supplyAsync(() -> overpassClient.query(endpoint, query), spotExecutor))
                .toList();
        attempts.forEach(attempt -> attempt.whenComplete((response, error) -> {
            if (error == null && response != null) {
                firstSuccess.complete(response);
            } else if (failures.incrementAndGet() == attempts.size()) {
                firstSuccess.completeExceptionally(error != null ? error : new IllegalStateException("빈 응답"));
            }
        }));
        try {
            OverpassResponse response = firstSuccess.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return response.elements() == null ? List.of() : response.elements();
        } catch (TimeoutException e) {
            throw new SpotsUnavailableException("모든 서버가 " + timeoutMillis + "ms 안에 응답하지 않았습니다", e);
        } catch (ExecutionException e) {
            throw new SpotsUnavailableException("모든 서버가 실패했습니다", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SpotsUnavailableException("검색이 중단되었습니다", e);
        } finally {
            // 승자가 정해졌거나 포기했으면 남은 요청 결과는 쓰지 않는다
            attempts.forEach(attempt -> attempt.cancel(true));
        }
    }

    static SpotResponse toResponse(SpotCategory category, List<Element> elements, double lat, double lon,
                                   boolean stale) {
        // 같은 이름이 여러 번 나오면 가장 가까운 것 하나만
        Map<String, SpotResponse.Spot> nearestByName = new LinkedHashMap<>();
        elements.stream()
                .filter(e -> e.tags() != null && e.tags().get("name") != null && !e.tags().get("name").isBlank())
                .map(e -> toSpot(e, lat, lon))
                .sorted(Comparator.comparingInt(SpotResponse.Spot::distanceMeters))
                .forEach(spot -> nearestByName.putIfAbsent(spot.name(), spot));
        List<SpotResponse.Spot> spots = nearestByName.values().stream().limit(MAX_SPOTS).toList();
        return new SpotResponse(category.name().toLowerCase(), spots, stale);
    }

    private static SpotResponse.Spot toSpot(Element element, double lat, double lon) {
        int meters = (int) Math.round(haversineMeters(lat, lon, element.lat(), element.lon()));
        int minutes = Math.max(1, (int) Math.round(meters / WALK_METERS_PER_MINUTE));
        return new SpotResponse.Spot(element.tags().get("name"), SpotCategory.label(element.tags().get("amenity")),
                meters, minutes);
    }

    static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
