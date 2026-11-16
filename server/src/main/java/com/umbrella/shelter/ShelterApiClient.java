package com.umbrella.shelter;

import java.util.List;
import java.util.Map;

public interface ShelterApiClient {

    Page fetch(int pageNo, int pageSize);

    record Page(List<Map<String, Object>> rows, int totalCount) {
    }

    // 하루 요청 한도 초과(22)나 키 거부(20·30·31)처럼 다시 시도해도 소용없는 응답
    class RefusedException extends RuntimeException {

        private final String code;

        public RefusedException(String code, String message) {
            super("[" + code + "] " + message);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
