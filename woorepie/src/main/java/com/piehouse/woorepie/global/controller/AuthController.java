package com.piehouse.woorepie.global.controller;

import com.piehouse.woorepie.global.kafka.service.AuthService;
import com.piehouse.woorepie.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuthStatus(@AuthenticationPrincipal Object session, HttpServletRequest request) {

        Map<String, Object> result = authService.getAuthStatus(session);
        String message = (boolean) result.getOrDefault("authenticated", false) ? "Authenticated" : "Unauthenticated";

        ApiResponse<Map<String, Object>> response = ApiResponse.of(HttpStatus.OK.value(), message, request.getRequestURI(), result);

        return ResponseEntity.ok(response);
    }

}
