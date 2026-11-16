package com.umbrella.shelter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.umbrella.config.UmbrellaProperties;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpShelterApiClient implements ShelterApiClient {

    private static final Set<String> REFUSED_CODES = Set.of("20", "22", "30", "31");

    private final RestClient restClient;
    private final UmbrellaProperties.Shelter shelter;
    private final String serviceKey;

    public HttpShelterApiClient(RestClient.Builder externalRestClientBuilder, UmbrellaProperties properties) {
        this.shelter = properties.shelter();
        this.restClient = externalRestClientBuilder.clone().baseUrl(shelter.baseUrl()).build();
        // 포털에서 인코딩된 키(%2B 등)를 복사해 넣어도 두 번 인코딩되지 않게 풀어 둔다
        String key = shelter.serviceKey() == null ? "" : shelter.serviceKey();
        this.serviceKey = key.contains("%") ? URLDecoder.decode(key, StandardCharsets.UTF_8) : key;
    }

    @Override
    public Page fetch(int pageNo, int pageSize) {
        Response response = restClient.get()
                .uri(uri -> uri.path("/" + shelter.serviceId())
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("returnType", "json")
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", pageSize)
                        .build())
                .retrieve()
                .body(Response.class);
        if (response == null || response.header() == null) {
            throw new IllegalStateException("무더위쉼터 응답이 비어 있습니다");
        }
        String code = response.header().resultCode();
        if (!"00".equals(code)) {
            String message = response.header().errorMsg() != null ? response.header().errorMsg()
                    : response.header().resultMsg();
            if (REFUSED_CODES.contains(code)) {
                throw new RefusedException(code, message);
            }
            throw new IllegalStateException("[" + code + "] " + message);
        }
        return new Page(response.body() == null ? List.of() : response.body(), response.totalCount());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(Header header, List<Map<String, Object>> body, int totalCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Header(String resultCode, String resultMsg, String errorMsg) {
    }
}
