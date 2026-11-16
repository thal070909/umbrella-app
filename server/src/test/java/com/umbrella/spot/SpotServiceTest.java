package com.umbrella.spot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.umbrella.config.UmbrellaProperties;
import com.umbrella.shelter.Shelter;
import com.umbrella.shelter.ShelterIndex;
import com.umbrella.shelter.ShelterSync;
import com.umbrella.spot.KakaoPlaceClient.KakaoPlace;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import tools.jackson.databind.json.JsonMapper;
import com.umbrella.spot.OverpassClient.Element;
import com.umbrella.spot.OverpassClient.OverpassResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SpotServiceTest {

    private static final double LAT = 37.5665;
    private static final double LON = 126.9780;
    private static final List<String> ENDPOINTS = List.of("slow", "broken", "fast");

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @AfterEach
    void tearDown() {
        executor.close();
    }

    // 서버마다 지연·실패를 따로 정할 수 있는 가짜 Overpass
    static class FakeOverpass implements OverpassClient {
        final Map<String, Long> delayMillis = new ConcurrentHashMap<>();
        final Map<String, Boolean> fails = new ConcurrentHashMap<>();
        final AtomicInteger calls = new AtomicInteger();
        final OverpassResponse response = new OverpassResponse(List.of(
                new Element(1, LAT + 0.003, LON, Map.of("name", "먼 카페", "amenity", "cafe")),
                new Element(2, LAT + 0.0005, LON, Map.of("name", "가까운 식당", "amenity", "restaurant")),
                new Element(3, LAT, LON, Map.of("amenity", "cafe"))));

        @Override
        public OverpassResponse query(String endpoint, String query) {
            calls.incrementAndGet();
            try {
                Thread.sleep(delayMillis.getOrDefault(endpoint, 0L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (fails.getOrDefault(endpoint, false)) {
                throw new IllegalStateException(endpoint + " 과부하");
            }
            return response;
        }
    }

    // 카테고리 코드별 결과·실패를 정할 수 있는 가짜 카카오
    static class FakeKakao implements KakaoPlaceClient {
        final Map<String, List<KakaoPlace>> results = new ConcurrentHashMap<>();
        boolean fails;
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public List<KakaoPlace> searchCategory(String code, double lat, double lon, int radiusMeters) {
            calls.incrementAndGet();
            if (fails) {
                throw new IllegalStateException("429 한도 초과");
            }
            return results.getOrDefault(code, List.of());
        }
    }

    private SpotService service(FakeOverpass fake, Duration timeout) {
        return service(fake, new FakeKakao(), "", timeout);
    }

    private SpotService service(FakeOverpass overpass, FakeKakao kakao, String kakaoKey, Duration timeout) {
        UmbrellaProperties properties = properties(kakaoKey, timeout);
        // 쉼터 키가 없는 상태: 무더위쉼터는 OpenStreetMap으로 찾는다
        ShelterSync noShelters = new ShelterSync((page, size) -> { throw new IllegalStateException(); }, properties,
                JsonMapper.builder().build(), executor, CLOCK);
        return new SpotService(overpass, kakao, noShelters, executor, CLOCK, properties);
    }

    private static UmbrellaProperties properties(String kakaoKey, Duration timeout) {
        return new UmbrellaProperties(List.of(), "test", 60, null,
                new UmbrellaProperties.Spots(ENDPOINTS, timeout, Duration.ofMinutes(30), Duration.ofHours(24)),
                new UmbrellaProperties.Kakao(kakaoKey, "http://kakao.test", Duration.ofSeconds(2)),
                new UmbrellaProperties.Shelter("", "http://shelter.test", "DSSP-IF-10942", 1000, "build/none.json",
                        Duration.ofHours(24)));
    }

    // 2026-07-22(수) 오후 2시
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-22T05:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void 무더위쉼터는_지금_운영_중인_곳을_먼저_보여주고_문_닫은_곳은_뒤로_보낸다() {
        ShelterIndex index = new ShelterIndex(List.of(
                new Shelter("1", "가까운데 문 닫은 경로당", LAT + 0.001, LON, "주소1", "경로당", "0900", "1200", false, false, true),
                new Shelter("2", "조금 먼 주민센터", LAT + 0.004, LON, "주소2", "주민센터", "0900", "1800", true, false, true),
                new Shelter("3", "시간 모르는 도서관", LAT + 0.002, LON, "주소3", "도서관", null, null, false, false, false)));
        SpotService service = service(new FakeOverpass(), Duration.ofSeconds(8));

        SpotResponse response = service.fromShelters(index, LAT, LON, SpotCategory.HEAT);

        assertThat(response.source()).isEqualTo("public-data");
        assertThat(response.spots()).extracting(SpotResponse.Spot::name)
                .containsExactly("조금 먼 주민센터", "시간 모르는 도서관", "가까운데 문 닫은 경로당");
        assertThat(response.spots().get(0).openNow()).isTrue();
        assertThat(response.spots().get(0).detail()).isEqualTo("평일 09:00~18:00 · 주말 개방 · 냉방기");
        assertThat(response.spots().get(1).openNow()).isNull();
    }

    @Test
    void 카카오_키가_있으면_비_피하기는_카페와_음식점을_합쳐_가까운_순으로_보여준다() {
        FakeOverpass overpass = new FakeOverpass();
        FakeKakao kakao = new FakeKakao();
        kakao.results.put("CE7", List.of(new KakaoPlace("빗방울 카페", "카페", "240", "서울 중구 명동길 1", null)));
        kakao.results.put("FD6", List.of(
                new KakaoPlace("우산 국밥", "음식점", "90", null, "서울 중구 명동2가 3"),
                new KakaoPlace("빗방울 카페", "카페", "500", "서울 중구 다른길 9", null)));

        SpotResponse response = service(overpass, kakao, "test-key", Duration.ofSeconds(8))
                .spots(LAT, LON, SpotCategory.RAIN, false);

        assertThat(response.source()).isEqualTo("kakao");
        assertThat(response.spots()).extracting(SpotResponse.Spot::name).containsExactly("우산 국밥", "빗방울 카페");
        assertThat(response.spots().get(0).detail()).isEqualTo("서울 중구 명동2가 3");
        assertThat(response.spots().get(1).walkMinutes()).isEqualTo(4);
        assertThat(overpass.calls.get()).isZero();
    }

    @Test
    void 카카오가_실패하면_OpenStreetMap으로_이어서_찾는다() {
        FakeOverpass overpass = new FakeOverpass();
        FakeKakao kakao = new FakeKakao();
        kakao.fails = true;

        SpotResponse response = service(overpass, kakao, "test-key", Duration.ofSeconds(8))
                .spots(LAT, LON, SpotCategory.RAIN, false);

        assertThat(response.source()).isEqualTo("openstreetmap");
        assertThat(response.spots()).isNotEmpty();
    }

    @Test
    void 카카오_결과는_저장하지_않아서_다시_들어오면_다시_부른다() {
        FakeKakao kakao = new FakeKakao();
        SpotService service = service(new FakeOverpass(), kakao, "test-key", Duration.ofSeconds(8));

        service.spots(LAT, LON, SpotCategory.RAIN, false);
        service.spots(LAT, LON, SpotCategory.RAIN, false);

        assertThat(kakao.calls.get()).isEqualTo(4);
    }

    @Test
    void 카카오_키가_없으면_카카오를_부르지_않는다() {
        FakeKakao kakao = new FakeKakao();

        service(new FakeOverpass(), kakao, "", Duration.ofSeconds(8)).spots(LAT, LON, SpotCategory.RAIN, false);

        assertThat(kakao.calls.get()).isZero();
    }

    @Test
    void 한_서버가_느리고_한_서버가_실패해도_가장_빠른_성공_응답으로_바로_보여준다() {
        FakeOverpass fake = new FakeOverpass();
        fake.delayMillis.put("slow", 5_000L);
        fake.fails.put("broken", true);
        fake.delayMillis.put("fast", 50L);

        long start = System.nanoTime();
        SpotResponse response = service(fake, Duration.ofSeconds(8)).spots(LAT, LON, SpotCategory.RAIN, false);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(2_000);
        assertThat(response.stale()).isFalse();
        // 이름 없는 장소는 빼고, 가까운 순서로
        assertThat(response.spots()).extracting(SpotResponse.Spot::name).containsExactly("가까운 식당", "먼 카페");
        assertThat(response.spots().get(0).category()).isEqualTo("음식점");
    }

    @Test
    void 모든_서버가_제한_시간_안에_응답하지_않으면_그만_기다린다() {
        FakeOverpass fake = new FakeOverpass();
        ENDPOINTS.forEach(endpoint -> fake.delayMillis.put(endpoint, 5_000L));

        long start = System.nanoTime();
        assertThatThrownBy(() -> service(fake, Duration.ofMillis(300)).spots(LAT, LON, SpotCategory.RAIN, false))
                .isInstanceOf(SpotsUnavailableException.class);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(2_000);
    }

    @Test
    void 같은_블록에서_다시_들어오면_외부_서버를_다시_부르지_않는다() {
        FakeOverpass fake = new FakeOverpass();
        SpotService service = service(fake, Duration.ofSeconds(8));

        service.spots(LAT, LON, SpotCategory.RAIN, false);
        int callsAfterFirst = fake.calls.get();
        service.spots(LAT + 0.0001, LON, SpotCategory.RAIN, false);

        assertThat(fake.calls.get()).isEqualTo(callsAfterFirst);
    }

    @Test
    void 세_서버가_모두_실패하면_전에_받아_둔_결과를_stale로_보여준다() {
        FakeOverpass fake = new FakeOverpass();
        SpotService service = service(fake, Duration.ofSeconds(8));
        service.spots(LAT, LON, SpotCategory.RAIN, false);

        ENDPOINTS.forEach(endpoint -> fake.fails.put(endpoint, true));
        SpotResponse response = service.spots(LAT, LON, SpotCategory.RAIN, true);

        assertThat(response.stale()).isTrue();
        assertThat(response.spots()).isNotEmpty();
    }

    @Test
    void 받아_둔_결과도_없이_모두_실패하면_오류로_알린다() {
        FakeOverpass fake = new FakeOverpass();
        ENDPOINTS.forEach(endpoint -> fake.fails.put(endpoint, true));

        assertThatThrownBy(() -> service(fake, Duration.ofSeconds(8)).spots(LAT, LON, SpotCategory.HEAT, false))
                .isInstanceOf(SpotsUnavailableException.class);
    }
}
