package com.piehouse.woorepie.global.controller;

import com.piehouse.woorepie.global.dto.request.S3AgentRequest;
import com.piehouse.woorepie.global.dto.request.S3CustomerRequest;
import com.piehouse.woorepie.global.dto.request.S3EstateRequest;
import com.piehouse.woorepie.global.dto.response.S3UrlResponse;
import com.piehouse.woorepie.global.response.ApiResponse;
import com.piehouse.woorepie.global.response.ApiResponseUtil;
import com.piehouse.woorepie.global.service.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/s3-presigned-url")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/customer")
    public ResponseEntity<ApiResponse<S3UrlResponse>> getCustomerPresignedUrl(@Valid @RequestBody S3CustomerRequest s3request, HttpServletRequest request) {
        log.info("AWS S3 customer Presigned Url request");
        S3UrlResponse s3UrlResponse = s3Service.generateCustomerPresignedUrl("customer", s3request);
        return ApiResponseUtil.success(s3UrlResponse, request);
    }

    @PostMapping("/agent")
    public ResponseEntity<ApiResponse<List<S3UrlResponse>>> getAgentPresignedUrl(@Valid @RequestBody S3AgentRequest s3request, HttpServletRequest request) {
        log.info("AWS S3 agent Presigned Url request");
        List<S3UrlResponse> s3UrlResponselist = s3Service.generateAgentPresignedUrl("agent", s3request);
        return ApiResponseUtil.success(s3UrlResponselist, request);
    }

    @PostMapping("/estate")
    public ResponseEntity<ApiResponse<List<S3UrlResponse>>> getEstatePresignedUrl(@Valid @RequestBody S3EstateRequest s3request, HttpServletRequest request) {
        log.info("AWS S3 estate Presigned Url request");
        List<S3UrlResponse> s3UrlResponselist = s3Service.generateEstatePresignedUrl("estate", s3request);
        return ApiResponseUtil.success(s3UrlResponselist, request);
    }

}
