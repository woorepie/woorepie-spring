package com.piehouse.woorepie.customer.controller;

import com.piehouse.woorepie.customer.dto.SessionCustomer;
import com.piehouse.woorepie.customer.dto.request.CreateCustomerRequest;
import com.piehouse.woorepie.customer.dto.request.ModifyPassword;
import com.piehouse.woorepie.customer.dto.response.GetCustomerSubscriptionResponse;
import com.piehouse.woorepie.customer.dto.request.LoginCustomerRequest;
import com.piehouse.woorepie.customer.dto.response.GetCustomerAccountResponse;
import com.piehouse.woorepie.customer.dto.response.GetCustomerResponse;
import com.piehouse.woorepie.customer.dto.response.GetCustomerTradeResponse;
import com.piehouse.woorepie.customer.service.CustomerService;
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

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@Valid @RequestBody LoginCustomerRequest requestDto, HttpServletRequest request) {
        log.info("Login customer request");
        customerService.customerLogin(requestDto, request);
        return ApiResponseUtil.success(null, request);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        log.info("Logout customer request");
        customerService.customerLogout(request);
        return ApiResponseUtil.success(null, request);
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkCustomerEmail(@RequestParam String customerEmail, HttpServletRequest request) {
        log.info("check customer email request");
        Boolean check = customerService.checkCustomerEmail(customerEmail);
        return ApiResponseUtil.success(check, request);
    }

    // 비밀번호 변경(로그인 상태)
    @PostMapping("/modify/password")
    public ResponseEntity<ApiResponse<Void>> login(@AuthenticationPrincipal SessionCustomer session,
                                                   @Valid @RequestBody ModifyPassword requestDto,
                                                   HttpServletRequest request) {
        log.info("update customer password request");
        customerService.modifyCustomerPassword(session.getCustomerId(), requestDto);
        return ApiResponseUtil.success(null, request);
    }

    // 회원 가입
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Void>> createCustomer(@Valid @RequestBody CreateCustomerRequest requestDto, HttpServletRequest request) {
        log.info("Signing customer request");
        customerService.createCustomer(requestDto);
        return ApiResponseUtil.of(HttpStatus.CREATED,"고객 가입 성공", null, request);
    }

    // 마이페이지 회원 정보 조회
    @GetMapping
    public ResponseEntity<ApiResponse<GetCustomerResponse>> getCustomer(@AuthenticationPrincipal SessionCustomer session, HttpServletRequest request) {
        log.info("Get customer request");
        GetCustomerResponse getCustomerResponse = customerService.getCustomer(session.getCustomerId());
        return ApiResponseUtil.success(getCustomerResponse, request);
    }

    // 마이페이지 계좌 내역 조회
    @GetMapping("/account")
    public ResponseEntity<ApiResponse<List<GetCustomerAccountResponse>>> getCustomerAccount(@AuthenticationPrincipal SessionCustomer session, HttpServletRequest request) {
        log.info("Get customer account request");
        List<GetCustomerAccountResponse> getCustomerAccountResponseList = customerService.getCustomerAccount(session.getCustomerId());
        return ApiResponseUtil.success(getCustomerAccountResponseList, request);
    }

    // 마이페이지 청약 내역 조회
    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<List<GetCustomerSubscriptionResponse>>> getCustomerSubscription(@AuthenticationPrincipal SessionCustomer session, HttpServletRequest request) {
        log.info("Get customer subscription request");
        List<GetCustomerSubscriptionResponse> getCustomerSubscriptionResponseList = customerService.getCustomerSubscription(session.getCustomerId());
        return ApiResponseUtil.success(getCustomerSubscriptionResponseList, request);
    }

    // 마이페이지 거래 내역 조회
    @GetMapping("/trade")
    public ResponseEntity<ApiResponse<List<GetCustomerTradeResponse>>> getCustomerTrade(@AuthenticationPrincipal SessionCustomer session, HttpServletRequest request) {
        log.info("Get customer trade request");
        List<GetCustomerTradeResponse> getCustomerTradeResponseList = customerService.getCustomerTrade(session.getCustomerId());
        return ApiResponseUtil.success(getCustomerTradeResponseList, request);
    }

}


