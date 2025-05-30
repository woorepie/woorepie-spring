package com.piehouse.woorepie.trade.controller;

import com.piehouse.woorepie.global.response.ApiResponse;
import com.piehouse.woorepie.global.response.ApiResponseUtil;
import com.piehouse.woorepie.trade.dto.request.*;
import com.piehouse.woorepie.trade.service.TradeRedisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class TradeRedisController { // Redis 동작 테스트용 Controller

    private final TradeRedisService tradeRedisService;

    // 매수 주문 저장 (매물/고객 기준 동시에)
    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<String>> saveBuyOrder(@RequestBody BuyEstateRequest request, @RequestHeader("customerId") Long customerId, HttpServletRequest httpRequest) {
        tradeRedisService.saveBuyOrder(request.getEstateId(), customerId, request.getTradeTokenAmount(), request.getTokenPrice());
        return ApiResponseUtil.success("매수 주문 저장 완료", httpRequest);
    }

    // 매도 주문 저장 (매물/고객 기준 동시에)
    @PostMapping("/sell")
    public ResponseEntity<ApiResponse<String>> saveSellOrder(@RequestBody SellEstateRequest request, @RequestHeader("customerId") Long customerId, HttpServletRequest httpRequest) {
        tradeRedisService.saveSellOrder(request.getEstateId(), customerId, request.getTradeTokenAmount(), request.getTokenPrice());
        return ApiResponseUtil.success("매도 주문 저장 완료", httpRequest);
    }

    // 매물 기준 매수 주문 전체 조회
    @GetMapping("/estate/{estateId}/buy")
    public ResponseEntity<ApiResponse<List<RedisEstateTradeValue>>> getEstateBuyOrders(@PathVariable Long estateId, HttpServletRequest request) {
        List<RedisEstateTradeValue> orders = tradeRedisService.getEstateBuyOrders(estateId);
        return ApiResponseUtil.success(orders, request);
    }

    // 매물 기준 매도 주문 전체 조회
    @GetMapping("/estate/{estateId}/sell")
    public ResponseEntity<ApiResponse<List<RedisEstateTradeValue>>> getEstateSellOrders(@PathVariable Long estateId, HttpServletRequest request) {
        List<RedisEstateTradeValue> orders = tradeRedisService.getEstateSellOrders(estateId);
        return ApiResponseUtil.success(orders, request);
    }

    // 고객 기준 매수 주문 전체 조회
    @GetMapping("/customer/buy")
    public ResponseEntity<ApiResponse<List<RedisCustomerTradeValue>>> getCustomerBuyOrders(@RequestHeader("customerId") Long customerId, HttpServletRequest request) {
        List<RedisCustomerTradeValue> orders = tradeRedisService.getCustomerBuyOrders(customerId);
        return ApiResponseUtil.success(orders, request);
    }

    // 고객 기준 매도 주문 전체 조회
    @GetMapping("/customer/sell")
    public ResponseEntity<ApiResponse<List<RedisCustomerTradeValue>>> getCustomerSellOrders(@RequestHeader("customerId") Long customerId, HttpServletRequest request) {
        List<RedisCustomerTradeValue> orders = tradeRedisService.getCustomerSellOrders(customerId);
        return ApiResponseUtil.success(orders, request);
    }

    // 매물 기준 가장 오래된 매수 주문 꺼내기 + 삭제 (고객 기준 동시 삭제)
    @GetMapping("/estate/{estateId}/buy/pop-oldest")
    public ResponseEntity<ApiResponse<RedisEstateTradeValue>> popOldestBuyOrder(@PathVariable Long estateId, HttpServletRequest request) {
        RedisEstateTradeValue order = tradeRedisService.popOldestBuyOrderFromBoth(estateId);
        return ApiResponseUtil.success(order, request);
    }

    // 매물 기준 가장 오래된 매도 주문 꺼내기 + 삭제 (고객 기준 동시 삭제)
    @GetMapping("/estate/{estateId}/sell/pop-oldest")
    public ResponseEntity<ApiResponse<RedisEstateTradeValue>> popOldestSellOrder(@PathVariable Long estateId, HttpServletRequest request) {
        RedisEstateTradeValue order = tradeRedisService.popOldestSellOrderFromBoth(estateId);
        return ApiResponseUtil.success(order, request);
    }
}
