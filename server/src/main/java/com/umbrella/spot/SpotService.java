package com.umbrella.spot;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.umbrella.common.GeoGrid;
import com.umbrella.config.UmbrellaProperties;
import com.umbrella.shelter.Shelter;
import com.umbrella.shelter.ShelterHours;
import com.umbrella.shelter.ShelterIndex;
import com.umbrella.shelter.ShelterSync;
import com.umbrella.spot.KakaoPlaceClient.KakaoPlace;
import java.time.Clock;
import java.time.LocalDateTime;
import com.umbrella.spot.OverpassClient.Element;
import com.umbrella.spot.OverpassClient.OverpassResponse;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SpotService {

    private static final Logger log = LoggerFactory.getLogger(SpotService.class);

    // 약 200m. 같은 블록에 있는 사람은 같은 검색 결과를 나눠 쓴다 (거리는 각자 위치로 다시 계산)
    static final double GRID_DEGREES = 0.002;
    static final int MAX_SPOTS = 15;
    // 사람 걷는 속도 분당 약 67m
    static final double WALK_METERS_PER_MINUTE = 67;
    // 비 피하기: 카카오 카테고리 CE7(카페), FD6(음식점)
    static final List<String> KAKAO_RAIN_CODES = List.of("CE7", "FD6");

    // 비를 피하러 들어갈 수 없는 곳 (직원만 쓰는 식당)
    static final List<String> NOT_OPEN_TO_PUBLIC = List.of("구내식당", "직원식당", "사내식당");

    static final String SOURCE_KAKAO = "kakao";
    static final String SOURCE_OSM = "openstreetmap";
    static final String SOURCE_PUBLIC_DATA = "public-data";
    // 반경 안에서 가까운 순으로 이만큼 뽑은 뒤, 운영 중인 곳을 앞으로 다시 줄 세운다
    static final int SHELTER_CANDIDATES = 40;

    private final OverpassClient overpassClient;
    private final KakaoPlaceClient kakaoPlaceClient;
    private final ShelterSync shelterSync;
    private final Clock clock;
    private final ExecutorService spotExecutor;
    private final List<String> endpoints;
    private final long timeoutMillis;
    private final UmbrellaProperties.Kakao kakao;
    private final Cache<String, List<Element>> fresh;
    // 세 서버가 모두 실패할 때 꺼내 쓰는 예전 결과. fresh보다 오래 들고 있는다
    private final Cache<String, List<Element>> stale;

    public SpotService(OverpassClient overpassClient, KakaoPlaceClient kakaoPlaceClient, ShelterSync shelterSync,
                       ExecutorService spotExecutor, Clock clock, UmbrellaProperties properties) {
        this.overpassClient = overpassClient;
        this.kakaoPlaceClient = kakaoPlaceClient;
        this.shelterSync = shelterSync;
        this.clock = clock;
        this.spotExecutor = spotExecutor;
        this.endpoints = properties.spots().endpoints();
        this.timeoutMillis = properties.spots().timeout().toMillis();
        this.kakao = properties.kakao();
        this.fresh = Caffeine.newBuilder().expireAfterWrite(properties.spots().cacheTtl()).maximumSize(20_000).build();
        this.stale = Caffeine.newBuilder().expireAfterWrite(properties.spots().staleTtl()).maximumSize(20_000).build();
    }

    // refresh = 사용자가 '다시 찾기'를 눌렀을 때. 캐시를 건너뛰고 새로 찾는다
    public SpotResponse spots(double lat, double lon, SpotCategory category, boolean refresh) {
        // 무더위쉼터는 공공데이터를 받아 두었으면 그걸 쓴다 (외부 호출 없이 메모리에서 바로)
        if (category == SpotCategory.HEAT) {
            Optional<ShelterIndex> shelters = shelterSync.index();
            if (shelters.isPresent()) {
                return fromShelters(shelters.get(), lat, lon, category);
            }
        }
        // 비 피하기는 국내 상호가 촘촘한 카카오를 먼저 쓰고, 실패하면 OpenStreetMap으로 이어 간다
        if (category == SpotCategory.RAIN && kakao != null && kakao.enabled()) {
            try {
                return fromKakao(lat, lon, category);
            } catch (RuntimeException e) {
                log.warn("카카오 장소 검색 실패, OpenStreetMap으로 대신 찾음: {}", e.getMessage());
            }
        }
        return fromOverpass(lat, lon, category, refresh);
    }

    // 카카오 로컬 결과는 약관상 저장하지 않는다. 캐시 없이 요청마다 카페·음식점을 동시에 찾는다
    SpotResponse fromKakao(double lat, double lon, SpotCategory category) {
        List<CompletableFuture<List<KakaoPlace>>> searches = KAKAO_RAIN_CODES.stream()
                .map(code -> CompletableFuture.supplyAsync(
                        () -> kakaoPlaceClient.searchCategory(code, lat, lon, category.radiusMeters()), spotExecutor))
                .toList();
        try {
            CompletableFuture.allOf(searches.toArray(CompletableFuture[]::new))
                    .get(kakao.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IllegalStateException("카카오가 " + kakao.timeout().toMillis() + "ms 안에 응답하지 않았습니다", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("카카오 검색 실패: " + e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("카카오 검색이 중단되었습니다", e);
        } finally {
            searches.forEach(search -> search.cancel(true));
        }
        Stream<SpotResponse.Spot> spots = searches.stream()
                .flatMap(search -> search.join().stream())
                .filter(place -> isOpenToPublic(place.placeName()))
                .map(SpotService::toSpot);
        return new SpotResponse(category.name().toLowerCase(), nearestUnique(spots), SOURCE_KAKAO, false);
    }

    private SpotResponse fromOverpass(double lat, double lon, SpotCategory category, boolean refresh) {
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
        Stream<SpotResponse.Spot> spots = elements.stream()
                .filter(e -> e.tags() != null && e.tags().get("name") != null && !e.tags().get("name").isBlank())
                .map(e -> toSpot(e, lat, lon));
        return new SpotResponse(category.name().toLowerCase(), nearestUnique(spots), SOURCE_OSM, stale);
    }

    // 같은 이름이 여러 번 나오면 가장 가까운 것 하나만, 가까운 순서로 15개
    private static List<SpotResponse.Spot> nearestUnique(Stream<SpotResponse.Spot> spots) {
        Map<String, SpotResponse.Spot> nearestByName = new LinkedHashMap<>();
        spots.sorted(Comparator.comparingInt(SpotResponse.Spot::distanceMeters))
                .forEach(spot -> nearestByName.putIfAbsent(spot.name(), spot));
        return nearestByName.values().stream().limit(MAX_SPOTS).toList();
    }

    private static SpotResponse.Spot toSpot(Element element, double lat, double lon) {
        int meters = (int) Math.round(haversineMeters(lat, lon, element.lat(), element.lon()));
        return new SpotResponse.Spot(element.tags().get("name"), SpotCategory.label(element.tags().get("amenity")),
                meters, walkMinutes(meters), null, null);
    }

    private static SpotResponse.Spot toSpot(KakaoPlace place) {
        int meters = parseMeters(place.distance());
        String address = place.roadAddressName() != null && !place.roadAddressName().isBlank()
                ? place.roadAddressName() : place.addressName();
        return new SpotResponse.Spot(place.placeName(), place.categoryGroupName(), meters, walkMinutes(meters), address,
                null);
    }

    // 무더위쉼터는 행정안전부 지정 쉼터 색인에서 찾는다
    // 지금 운영 중인 곳을 먼저, 그다음 운영시간을 모르는 곳, 문 닫은 곳은 뒤로 (헛걸음하지 않게)
    SpotResponse fromShelters(ShelterIndex index, double lat, double lon, SpotCategory category) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<SpotResponse.Spot> spots = index.nearest(lat, lon, category.radiusMeters(), SHELTER_CANDIDATES).stream()
                .map(nearby -> {
                    Shelter shelter = nearby.shelter();
                    int meters = nearby.distanceMeters();
                    String type = shelter.type() != null ? shelter.type() : "무더위쉼터";
                    return new SpotResponse.Spot(shelter.name(), type, meters, walkMinutes(meters),
                            ShelterHours.describe(shelter), ShelterHours.openNow(shelter, now));
                })
                .sorted(Comparator.comparingInt((SpotResponse.Spot spot) -> openRank(spot.openNow()))
                        .thenComparingInt(SpotResponse.Spot::distanceMeters))
                .limit(MAX_SPOTS)
                .toList();
        return new SpotResponse(category.name().toLowerCase(), spots, SOURCE_PUBLIC_DATA, false);
    }

    private static int openRank(Boolean openNow) {
        if (Boolean.TRUE.equals(openNow)) {
            return 0;
        }
        return openNow == null ? 1 : 2;
    }

    static boolean isOpenToPublic(String name) {
        return name != null && NOT_OPEN_TO_PUBLIC.stream().noneMatch(name::contains);
    }

    private static int parseMeters(String distance) {
        try {
            return Integer.parseInt(distance);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    static int walkMinutes(int meters) {
        return Math.max(1, (int) Math.round(meters / WALK_METERS_PER_MINUTE));
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
