package com.umbrella.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.umbrella.config.UmbrellaProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 한 사람(IP)이 /api를 1분에 정해진 횟수만 부르게 한다
// 새로고침을 연타하거나 누가 긁어 가도 카카오·공공데이터 하루 한도가 바닥나 모두가 못 쓰는 일을 막는다
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int limitPerMinute;
    // 1분 창마다 IP별 요청 수. 창이 지나면 저절로 사라진다
    private final Cache<String, AtomicInteger> counters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(100_000)
            .build();

    public RateLimitFilter(UmbrellaProperties properties) {
        this.limitPerMinute = properties.rateLimitPerMinute();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long window = System.currentTimeMillis() / 60_000;
        String key = clientIp(request) + ":" + window;
        int count = counters.get(key, k -> new AtomicInteger()).incrementAndGet();
        if (count > limitPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(60 - (System.currentTimeMillis() / 1000) % 60));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(
                    "{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"요청이 너무 많아요. 잠시 후 다시 시도해주세요.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    // X-Forwarded-For를 직접 읽으면 사용자가 헤더를 바꿔 제한을 피할 수 있다
    // 그래서 server.forward-headers-strategy=native로 믿을 수 있는 프록시가 붙인 값만 반영된 주소를 쓴다
    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
