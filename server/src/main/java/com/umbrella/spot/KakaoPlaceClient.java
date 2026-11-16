package com.umbrella.spot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public interface KakaoPlaceClient {

    // 카카오 로컬 카테고리 검색. code: CE7(카페), FD6(음식점)
    List<KakaoPlace> searchCategory(String code, double lat, double lon, int radiusMeters);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoPlace(
            @JsonProperty("place_name") String placeName,
            @JsonProperty("category_group_name") String categoryGroupName,
            String distance,
            @JsonProperty("road_address_name") String roadAddressName,
            @JsonProperty("address_name") String addressName) {
    }
}
