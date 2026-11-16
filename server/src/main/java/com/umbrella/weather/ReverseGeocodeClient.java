package com.umbrella.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.umbrella.config.UmbrellaProperties;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ReverseGeocodeClient {

    static final String UNKNOWN_PLACE = "현재 위치";

    private final RestClient restClient;

    public ReverseGeocodeClient(RestClient.Builder externalRestClientBuilder, UmbrellaProperties properties) {
        this.restClient = externalRestClientBuilder.clone().baseUrl(properties.weather().geocodeBaseUrl()).build();
    }

    // 동네 이름은 없어도 날씨는 보여 줘야 해서, 실패하면 예외 대신 "현재 위치"
    public String placeName(double lat, double lon) {
        try {
            Place place = restClient.get()
                    .uri(uri -> uri.path("/data/reverse-geocode-client")
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("localityLanguage", "ko")
                            .build())
                    .retrieve()
                    .body(Place.class);
            return place == null ? UNKNOWN_PLACE : place.label();
        } catch (RestClientException e) {
            return UNKNOWN_PLACE;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Place(String city, String locality, String principalSubdivision, LocalityInfo localityInfo) {

        // "명동 서울특별시"처럼 구·동 + 시
        String label() {
            String district = Optional.ofNullable(localityInfo)
                    .map(LocalityInfo::administrative)
                    .flatMap(list -> list.stream().filter(a -> a.adminLevel() == 8).map(Admin::name).findFirst())
                    .orElse(principalSubdivision);
            String cityName = city != null && !city.isBlank() ? city : locality;
            Set<String> parts = new LinkedHashSet<>();
            if (district != null && !district.isBlank()) {
                parts.add(district);
            }
            if (cityName != null && !cityName.isBlank()) {
                parts.add(cityName);
            }
            return parts.isEmpty() ? UNKNOWN_PLACE : String.join(" ", parts);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LocalityInfo(List<Admin> administrative) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Admin(int adminLevel, String name) {
    }
}
