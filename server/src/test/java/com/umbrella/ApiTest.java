package com.umbrella;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umbrella.spot.KakaoPlaceClient;
import com.umbrella.spot.OverpassClient;
import com.umbrella.weather.OpenMeteoClient;
import com.umbrella.weather.OpenMeteoClient.Current;
import com.umbrella.weather.OpenMeteoClient.Forecast;
import com.umbrella.weather.OpenMeteoClient.Hourly;
import com.umbrella.weather.ReverseGeocodeClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenMeteoClient openMeteoClient;

    @MockitoBean
    private ReverseGeocodeClient reverseGeocodeClient;

    @MockitoBean
    private OverpassClient overpassClient;

    @MockitoBean
    private KakaoPlaceClient kakaoPlaceClient;

    @Test
    void 날씨_API는_숫자와_함께_지금_할_일을_내려준다() throws Exception {
        Hourly hourly = new Hourly(
                List.of("2026-07-23T14:00", "2026-07-23T15:00", "2026-07-23T16:00"),
                List.of(20, 60, 80), List.of(0.0, 0.3, 1.0));
        given(openMeteoClient.fetch(anyDouble(), anyDouble()))
                .willReturn(new Forecast(new Current("2026-07-23T14:10", 31.4, 34.2, 0, 1, 70), hourly));
        given(reverseGeocodeClient.placeName(anyDouble(), anyDouble())).willReturn("명동 서울특별시");

        mockMvc.perform(get("/api/weather").param("lat", "37.5636").param("lon", "126.9834"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.place").value("명동 서울특별시"))
                .andExpect(jsonPath("$.temperature").value(31))
                .andExpect(jsonPath("$.umbrella.message").value("오후 3시부터 비 소식이 있어요, 우산을 챙기세요"))
                .andExpect(jsonPath("$.heatCarAlert").value(true));
    }

    @Test
    void 위치가_범위를_벗어나면_400과_안내_코드를_준다() throws Exception {
        mockMvc.perform(get("/api/weather").param("lat", "137").param("lon", "126.98"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 스팟을_못_불러오면_503과_다시_시도_안내를_준다() throws Exception {
        given(overpassClient.query(anyString(), anyString())).willThrow(new IllegalStateException("과부하"));

        mockMvc.perform(get("/api/spots").param("lat", "35.1").param("lon", "129.04").param("category", "heat"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SPOTS_UNAVAILABLE"));
    }
}
