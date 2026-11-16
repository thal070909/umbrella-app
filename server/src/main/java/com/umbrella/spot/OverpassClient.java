package com.umbrella.spot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

public interface OverpassClient {

    OverpassResponse query(String endpoint, String query);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OverpassResponse(List<Element> elements) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Element(long id, double lat, double lon, Map<String, String> tags) {
    }
}
