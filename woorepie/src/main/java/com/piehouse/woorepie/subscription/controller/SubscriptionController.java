package com.piehouse.woorepie.subscription.controller;

import com.piehouse.woorepie.agent.dto.SessionAgent;
import com.piehouse.woorepie.global.response.ApiResponse;
import com.piehouse.woorepie.global.response.ApiResponseUtil;
import com.piehouse.woorepie.subscription.dto.request.RegisterEstateRequest;
import com.piehouse.woorepie.subscription.dto.response.GetSubscriptionDetailsResponse;
import com.piehouse.woorepie.subscription.dto.response.GetSubscriptionSimpleResponse;
import com.piehouse.woorepie.subscription.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> registerEstate(
            @Valid @AuthenticationPrincipal SessionAgent sessionAgent,
            @RequestBody RegisterEstateRequest request,
            HttpServletRequest httpRequest
    ) {
        subscriptionService.registerEstate(request, sessionAgent.getAgentId());
        return ApiResponseUtil.of(HttpStatus.CREATED, "매물 청약 등록 성공", null, httpRequest);
    }

    // 청약중인 매물 리스트 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<GetSubscriptionSimpleResponse>>> getAllSubscriptionEstates(HttpServletRequest request) {
        List<GetSubscriptionSimpleResponse> responseList = subscriptionService.getActiveSubscriptions();
        return ApiResponseUtil.success(responseList, request);
    }

    // 청약 매물 상세 조회
    @GetMapping(params = "estateId")
    public ResponseEntity<ApiResponse<GetSubscriptionDetailsResponse>> getSubscriptionDetails(@RequestParam Long estateId, HttpServletRequest request) {
        GetSubscriptionDetailsResponse response = subscriptionService.getSubscriptionDetails(estateId);
        return ApiResponseUtil.success(response, request);
    }

}
