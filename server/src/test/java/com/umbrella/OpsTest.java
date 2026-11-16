package com.umbrella;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umbrella.shelter.ShelterApiClient;
import com.umbrella.spot.KakaoPlaceClient;
import com.umbrella.spot.OverpassClient;
import com.umbrella.weather.OpenMeteoClient;
import com.umbrella.weather.OpenMeteoClient.Current;
import com.umbrella.weather.OpenMeteoClient.Forecast;
import com.umbrella.weather.ReverseGeocodeClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "umbrella.rate-limit-per-minute=2")
@AutoConfigureMockMvc
class OpsTest {

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

    @MockitoBean
    private ShelterApiClient shelterApiClient;

    @Test
    void 헬스체크는_외부_API와_상관없이_UP을_준다() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void 한_사람이_1분_한도를_넘기면_429와_안내_코드를_준다() throws Exception {
        given(openMeteoClient.fetch(anyDouble(), anyDouble()))
                .willReturn(new Forecast(new Current("2026-07-23T14:10", 25, 25, 0, 0, 50), null));
        given(reverseGeocodeClient.placeName(anyDouble(), anyDouble())).willReturn("명동 서울특별시");

        // 1분 경계에 걸려 창이 바뀌어도 한 번은 넘도록 넉넉히 보낸다
        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            statuses.add(mockMvc.perform(get("/api/weather").param("lat", "37.56").param("lon", "126.98"))
                    .andReturn().getResponse().getStatus());
        }

        assertThat(statuses).contains(429);
        assertThat(statuses.get(0)).isEqualTo(200);
    }
}
