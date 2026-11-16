package com.umbrella.config;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UmbrellaProperties properties;

    public WebConfig(UmbrellaProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET");
    }

    // 외부 API가 연결조차 안 될 때 사용자를 오래 붙잡지 않게 연결 3초, 응답 6초로 끊는다
    @Bean
    public RestClient.Builder externalRestClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(6));
        // Overpass 공개 서버는 앱 이름(User-Agent) 없는 요청을 406으로 거절한다
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent());
    }

    // 미러 서버 동시 요청용. 기다리는 일이 대부분이라 가상 스레드로 충분하다
    @Bean(destroyMethod = "close")
    public ExecutorService spotExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // "지금 운영 중" 판단은 한국 시간 기준
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
