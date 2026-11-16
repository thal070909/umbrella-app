package com.umbrella.spot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.umbrella.config.UmbrellaProperties;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpKakaoPlaceClient implements KakaoPlaceClient {

    // 카카오 카테고리 검색은 한 번에 최대 15개
    private static final int PAGE_SIZE = 15;

    private final RestClient restClient;

    public HttpKakaoPlaceClient(RestClient.Builder externalRestClientBuilder, UmbrellaProperties properties) {
        this.restClient = externalRestClientBuilder.clone()
                .baseUrl(properties.kakao().baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.kakao().restApiKey())
                .build();
    }

    @Override
    public List<KakaoPlace> searchCategory(String code, double lat, double lon, int radiusMeters) {
        Documents documents = restClient.get()
                .uri(uri -> uri.path("/v2/local/search/category.json")
                        .queryParam("category_group_code", code)
                        .queryParam("x", lon)
                        .queryParam("y", lat)
                        .queryParam("radius", radiusMeters)
                        .queryParam("sort", "distance")
                        .queryParam("size", PAGE_SIZE)
                        .build())
                .retrieve()
                .body(Documents.class);
        return documents == null || documents.documents() == null ? List.of() : documents.documents();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Documents(List<KakaoPlace> documents) {
    }
}
