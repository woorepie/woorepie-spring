package com.piehouse.woorepie.agent.controller;

import com.piehouse.woorepie.agent.dto.SessionAgent;
import com.piehouse.woorepie.agent.dto.request.CreateAgentRequest;
import com.piehouse.woorepie.agent.dto.request.LoginAgentRequest;
import com.piehouse.woorepie.agent.dto.response.GetAgentResponse;
import com.piehouse.woorepie.agent.service.AgentService;
import com.piehouse.woorepie.global.response.ApiResponse;
import com.piehouse.woorepie.global.response.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;

    // 대행인 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> loginAgent(@Valid @RequestBody LoginAgentRequest loginAgentRequest, HttpServletRequest request) {
        log.info("Login agent request");
        agentService.loginAgent(loginAgentRequest, request);
        return ApiResponseUtil.success(null, request);
    }

    // 대행인 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logoutAgent(HttpServletRequest request) {
        log.info("Logout agent request");
        agentService.logoutAgent(request);
        return ApiResponseUtil.success(null, request);
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkAgentEmail(@RequestParam String agentEmail, HttpServletRequest request) {
        log.info("check agent email request");
        Boolean check = agentService.checkAgentEmail(agentEmail);
        return ApiResponseUtil.success(check, request);
    }

    // 대행인 회원가입
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Void>> createAgent(@Valid @RequestBody CreateAgentRequest agentRequest, HttpServletRequest request) {
        log.info("Signing agent request");
        agentService.createAgent(agentRequest, request);
        return ApiResponseUtil.of(HttpStatus.CREATED,"대행인 가입 성공", null, request);
    }

    // 대행인 정보 조회
    @GetMapping
    public ResponseEntity<ApiResponse<GetAgentResponse>> getAgentInfo(@AuthenticationPrincipal SessionAgent sessionAgent, HttpServletRequest request) {
        GetAgentResponse response = agentService.getAgentInfo(sessionAgent.getAgentId());
        return ApiResponseUtil.of(HttpStatus.OK, "대행인 정보 조회 성공", response, request);
    }

}
