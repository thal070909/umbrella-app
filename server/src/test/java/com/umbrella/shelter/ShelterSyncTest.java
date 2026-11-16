package com.umbrella.shelter;

import static org.assertj.core.api.Assertions.assertThat;

import com.umbrella.config.UmbrellaProperties;
import com.umbrella.shelter.ShelterApiClient.Page;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class ShelterSyncTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-22T05:00:00Z"), ZoneId.of("Asia/Seoul"));

    @TempDir
    Path tempDir;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @AfterEach
    void tearDown() {
        executor.close();
    }

    // 쪽당 2곳, 모두 5곳(좌표 없는 1곳 포함)을 주는 가짜 공공데이터 API
    static class FakeApi implements ShelterApiClient {
        final AtomicInteger calls = new AtomicInteger();
        Integer failOnPage;
        boolean refuse;

        @Override
        public Page fetch(int pageNo, int pageSize) {
            calls.incrementAndGet();
            if (refuse) {
                throw new RefusedException("22", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR");
            }
            if (failOnPage != null && failOnPage == pageNo) {
                throw new IllegalStateException("[99] 일시 오류");
            }
            List<Map<String, Object>> all = List.of(
                    row("1", "명동 경로당", "37.5636", "126.9834"),
                    row("2", "회현 주민센터", 37.5590, 126.9780),
                    row("3", "좌표 없는 쉼터", null, null),
                    row("4", "중구 도서관", "37.5641", "126.9970"),
                    row("5", "을지로 은행", "37.5660", "126.9910"));
            int from = (pageNo - 1) * pageSize;
            return new Page(all.subList(Math.min(from, all.size()), Math.min(from + pageSize, all.size())), all.size());
        }

        private static Map<String, Object> row(String id, String name, Object lat, Object lon) {
            Map<String, Object> row = new HashMap<>();
            row.put(ShelterMapper.ID, id);
            row.put(ShelterMapper.NAME, name);
            row.put(ShelterMapper.LAT, lat);
            row.put(ShelterMapper.LON, lon);
            row.put(ShelterMapper.WEEKDAY_OPEN, "0900");
            row.put(ShelterMapper.WEEKDAY_CLOSE, "1800");
            row.put(ShelterMapper.WEEKEND_OPEN, "Y");
            row.put(ShelterMapper.AIR_CONDITIONER, "2");
            return row;
        }
    }

    private ShelterSync sync(FakeApi api) {
        UmbrellaProperties properties = new UmbrellaProperties(List.of(), "test", 60, null, null, null,
                new UmbrellaProperties.Shelter("key", "http://shelter.test", "DSSP-IF-10942", 2,
                        tempDir.resolve("shelters.json").toString(), Duration.ofHours(24)));
        return new ShelterSync(api, properties, JsonMapper.builder().build(), executor, CLOCK);
    }

    @Test
    void 모든_쪽을_받아_좌표_있는_쉼터만_색인에_넣는다() {
        FakeApi api = new FakeApi();
        ShelterSync sync = sync(api);

        assertThat(sync.sync()).isTrue();

        assertThat(api.calls.get()).isEqualTo(3);
        assertThat(sync.index()).get().extracting(ShelterIndex::size).isEqualTo(4);
        assertThat(sync.index().get().nearest(37.5636, 126.9834, 1500, 10))
                .extracting(nearby -> nearby.shelter().name())
                .startsWith("명동 경로당");
    }

    @Test
    void 중간_쪽에서_실패하면_예전_색인을_그대로_쓴다() {
        FakeApi api = new FakeApi();
        ShelterSync sync = sync(api);
        sync.sync();
        ShelterIndex before = sync.index().orElseThrow();

        api.failOnPage = 2;

        assertThat(sync.sync()).isFalse();
        assertThat(sync.index()).containsSame(before);
    }

    @Test
    void 하루_한도_초과면_더_요청하지_않고_멈춘다() {
        FakeApi api = new FakeApi();
        api.refuse = true;

        assertThat(sync(api).sync()).isFalse();
        assertThat(api.calls.get()).isEqualTo(1);
    }

    @Test
    void 받은_쉼터를_파일로_남겨_다시_켤_때_API를_부르지_않고_쓴다() {
        sync(new FakeApi()).sync();
        FakeApi afterRestart = new FakeApi();
        ShelterSync restarted = sync(afterRestart);

        restarted.loadSnapshot();
        restarted.syncIfStale();

        assertThat(restarted.index()).get().extracting(ShelterIndex::size).isEqualTo(4);
        assertThat(afterRestart.calls.get()).isZero();
    }

    @Test
    void 반경_밖의_쉼터는_찾지_않는다() {
        List<Shelter> shelters = new ArrayList<>();
        shelters.add(new Shelter("1", "가까운 곳", 37.5636, 126.9834, null, null, null, null, false, false, false));
        shelters.add(new Shelter("2", "3km 떨어진 곳", 37.5906, 126.9834, null, null, null, null, false, false, false));

        assertThat(new ShelterIndex(shelters).nearest(37.5636, 126.9834, 1500, 10))
                .extracting(nearby -> nearby.shelter().name())
                .containsExactly("가까운 곳");
    }
}
