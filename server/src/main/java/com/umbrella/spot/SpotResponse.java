package com.umbrella.spot;

import java.util.List;

// source = 장소 정보 출처(kakao / openstreetmap / public-data). 화면에 출처를 표시한다
// stale = 검색 서버가 모두 실패해서, 전에 받아 둔 결과를 대신 보여 주는 중
public record SpotResponse(String category, List<Spot> spots, String source, boolean stale) {

    // detail = 이름 아래 한 줄 (주소, 운영시간 등). 없으면 null
    // openNow = 지금 운영 중인지 (true/false). 운영시간을 모르면 null
    public record Spot(String name, String category, int distanceMeters, int walkMinutes, String detail,
                       Boolean openNow) {
    }
}
