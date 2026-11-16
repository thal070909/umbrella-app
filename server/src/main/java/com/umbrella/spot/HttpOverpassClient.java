package com.umbrella.spot;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class HttpOverpassClient implements OverpassClient {

    private final RestClient restClient;

    public HttpOverpassClient(RestClient.Builder externalRestClientBuilder) {
        this.restClient = externalRestClientBuilder.clone().build();
    }

    @Override
    public OverpassResponse query(String endpoint, String query) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", query);
        return restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(OverpassResponse.class);
    }
}
