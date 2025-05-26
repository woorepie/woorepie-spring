package com.piehouse.woorepie.estate.controller;

import com.piehouse.woorepie.estate.dto.response.GetEstateDetailsResponse;
import com.piehouse.woorepie.estate.dto.response.GetEstateSimpleResponse;
import com.piehouse.woorepie.estate.service.EstateService;
import com.piehouse.woorepie.global.response.ApiResponse;
import com.piehouse.woorepie.global.response.ApiResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estate")
@RequiredArgsConstructor
public class EstateController {

    private final EstateService estateService;

    /**
     * 청약 완료된 매물 리스트 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GetEstateSimpleResponse>>> getTradableEstates(HttpServletRequest request) {
        List<GetEstateSimpleResponse> responseList = estateService.getTradableEstates();
        return ApiResponseUtil.success(responseList, request);
    }

    /**
     * 청약 완료된 매물 상세 조회
     */
    @GetMapping(params = "estateId")
    public ResponseEntity<ApiResponse<GetEstateDetailsResponse>> getEstateDetails(@RequestParam Long estateId, HttpServletRequest request) {
        GetEstateDetailsResponse response = estateService.getTradableEstateDetails(estateId);
        return ApiResponseUtil.success(response, request);
    }
}
