package com.umbrella.shelter;

// 무더위쉼터 한 곳. 운영시간은 원본 문자열 그대로 두고 ShelterHours가 해석한다
public record Shelter(
        String id,
        String name,
        double lat,
        double lon,
        String address,
        String type,
        String weekdayOpen,
        String weekdayClose,
        boolean weekendOpen,
        boolean nightOpen,
        boolean airConditioner) {
}
