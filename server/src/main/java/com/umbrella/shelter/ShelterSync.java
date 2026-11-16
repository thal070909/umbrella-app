package com.umbrella.shelter;

import com.umbrella.config.UmbrellaProperties;
import com.umbrella.shelter.ShelterApiClient.Page;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

// 전국 무더위쉼터를 통째로 받아 메모리 색인으로 들고 있는다
// - 서버를 켜면 저장해 둔 파일부터 읽어 바로 쓰고, 오래됐을 때만 API에서 다시 받는다 (하루 요청 한도 아끼기)
// - 다 받았을 때만 바꿔 끼운다. 중간에 실패하면 예전 색인을 그대로 쓴다
@Component
public class ShelterSync {

    private static final Logger log = LoggerFactory.getLogger(ShelterSync.class);

    private final ShelterApiClient client;
    private final UmbrellaProperties.Shelter properties;
    private final JsonMapper jsonMapper;
    private final ExecutorService executor;
    private final Clock clock;
    private final AtomicBoolean syncing = new AtomicBoolean();

    private volatile ShelterIndex index;
    private volatile Instant fetchedAt;

    public ShelterSync(ShelterApiClient client, UmbrellaProperties properties, JsonMapper jsonMapper,
                       ExecutorService spotExecutor, Clock clock) {
        this.client = client;
        this.properties = properties.shelter();
        this.jsonMapper = jsonMapper;
        this.executor = spotExecutor;
        this.clock = clock;
    }

    public Optional<ShelterIndex> index() {
        return Optional.ofNullable(index);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!properties.enabled()) {
            log.info("무더위쉼터 인증키가 없어 OpenStreetMap으로 찾습니다");
            return;
        }
        loadSnapshot();
        // 서버가 뜨는 걸 막지 않게 받기는 뒤에서 한다
        executor.execute(this::syncIfStale);
    }

    // 한 시간마다 확인해서 받아 둔 지 refresh-after가 지났으면 다시 받는다
    @Scheduled(initialDelayString = "PT1H", fixedDelayString = "PT1H")
    public void syncIfStale() {
        if (!properties.enabled()) {
            return;
        }
        Instant last = fetchedAt;
        if (last != null && last.plus(properties.refreshAfter()).isAfter(clock.instant())) {
            return;
        }
        sync();
    }

    boolean sync() {
        if (!syncing.compareAndSet(false, true)) {
            return false;
        }
        try {
            List<Shelter> shelters = fetchAll();
            index = new ShelterIndex(shelters);
            fetchedAt = clock.instant();
            saveSnapshot(shelters);
            log.info("무더위쉼터 {}곳을 받았습니다", shelters.size());
            return true;
        } catch (ShelterApiClient.RefusedException e) {
            // 한도 초과에서 다시 시도하면 남은 한도만 쓴다. 다음 확인 때까지 기다린다
            log.warn("무더위쉼터 API가 거절했습니다 {}. 예전 데이터를 계속 씁니다", e.getMessage());
            return false;
        } catch (RuntimeException e) {
            log.warn("무더위쉼터를 다 받지 못했습니다 ({}). 예전 데이터를 계속 씁니다", e.getMessage());
            return false;
        } finally {
            syncing.set(false);
        }
    }

    private List<Shelter> fetchAll() {
        int pageSize = properties.pageSize();
        Page first = client.fetch(1, pageSize);
        int pages = Math.max(1, (int) Math.ceil(first.totalCount() / (double) pageSize));
        List<Shelter> shelters = new ArrayList<>(first.totalCount());
        addAll(shelters, first.rows());
        for (int page = 2; page <= pages; page++) {
            addAll(shelters, client.fetch(page, pageSize).rows());
        }
        if (shelters.isEmpty()) {
            throw new IllegalStateException("받은 쉼터가 없습니다");
        }
        return shelters;
    }

    private static void addAll(List<Shelter> shelters, List<Map<String, Object>> rows) {
        rows.forEach(row -> ShelterMapper.map(row).ifPresent(shelters::add));
    }

    void loadSnapshot() {
        Path path = Path.of(properties.snapshotPath());
        if (!Files.exists(path)) {
            return;
        }
        try {
            Snapshot snapshot = jsonMapper.readValue(path.toFile(), Snapshot.class);
            index = new ShelterIndex(snapshot.shelters());
            fetchedAt = snapshot.fetchedAt();
            log.info("저장해 둔 무더위쉼터 {}곳을 읽었습니다 ({} 기준)", snapshot.shelters().size(), snapshot.fetchedAt());
        } catch (RuntimeException e) {
            log.warn("무더위쉼터 저장 파일을 읽지 못했습니다: {}", e.getMessage());
        }
    }

    private void saveSnapshot(List<Shelter> shelters) {
        Path path = Path.of(properties.snapshotPath());
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            // 쓰다 끊겨도 예전 파일이 깨지지 않게 임시 파일에 쓰고 바꿔 끼운다
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            jsonMapper.writeValue(temp.toFile(), new Snapshot(fetchedAt, shelters));
            Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            log.warn("무더위쉼터 저장 파일을 쓰지 못했습니다: {}", e.getMessage());
        }
    }

    record Snapshot(Instant fetchedAt, List<Shelter> shelters) {
    }
}
