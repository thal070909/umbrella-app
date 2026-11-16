package com.umbrella.spot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.umbrella.config.UmbrellaProperties;
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

    private SpotService service(FakeOverpass fake, Duration timeout) {
        UmbrellaProperties properties = new UmbrellaProperties(List.of(), "test", null,
                new UmbrellaProperties.Spots(ENDPOINTS, timeout, Duration.ofMinutes(30), Duration.ofHours(24)));
        return new SpotService(fake, executor, properties);
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
