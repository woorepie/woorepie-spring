package com.piehouse.woorepie.estate.service;

import com.piehouse.woorepie.estate.dto.RedisEstatePrice;

import java.util.List;
import java.util.Map;

public interface EstateRedisService {
    // 남은 토큰 수량 Redis에서 조회 및 초기화
    Long getRemainingTokensOrInit(Long estateId);

    // Lua Script 기반 감소 메서드
    Long decrementButNotNegative(Long estateId, long amount);

    // 토큰 수량 증가 (원자적 연산)
    Long incrementTokens(Long estateId, long amount);

    // 매물 시세 조회
    RedisEstatePrice getRedisEstatePrice(Long estateId);

    Map<Long, RedisEstatePrice> getMultipleRedisEstatePrice(List<Long> estateIds);

    void deleteRedisEstatePrice(Long estateId);

}
