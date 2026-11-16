package com.umbrella.common;

import com.umbrella.spot.SpotsUnavailableException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// 화면이 상황별 안내(다시 시도 등)를 고를 수 있게 code를 같이 내려준다
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record ErrorResponse(String code, String message) {
    }

    @ExceptionHandler(SpotsUnavailableException.class)
    public ResponseEntity<ErrorResponse> spotsUnavailable(SpotsUnavailableException e) {
        log.warn("스팟 검색 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("SPOTS_UNAVAILABLE", "근처 장소를 불러오지 못했어요. 잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler({RestClientException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> weatherUnavailable(RuntimeException e) {
        log.warn("외부 날씨 조회 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("WEATHER_UNAVAILABLE", "날씨를 불러오지 못했어요. 잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_INPUT", "위치나 검색 종류가 올바르지 않아요."));
    }
}
