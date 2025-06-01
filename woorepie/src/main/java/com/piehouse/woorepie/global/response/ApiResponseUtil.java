package com.piehouse.woorepie.global.response;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApiResponseUtil {

    public static <T> ResponseEntity<ApiResponse<T>> success(T data, HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiResponse.of(HttpStatus.OK.value(), "요청 성공", request.getRequestURI(), data)
        );
    }

    public static <T> ResponseEntity<ApiResponse<T>> of(HttpStatus status, String message, T data, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiResponse.of(status.value(), message, request.getRequestURI(), data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.of(status.value(), message, null, null));
    }
}
