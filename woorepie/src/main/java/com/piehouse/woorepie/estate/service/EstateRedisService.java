package com.piehouse.woorepie.estate.service;

import com.piehouse.woorepie.estate.dto.RedisEstatePrice;

import java.util.List;
import java.util.Map;

public interface EstateRedisService {
    // 청약 오픈 시 PostgreSQL에서 tokenAmount를 읽어와 Redis에 초기화
    void initializeRemainingTokens(Long estateId);

    // 남은 토큰 수량 Reids에 저장 (STRING)
    void setRemainingTokens(String estateId, long remainingTokens);

    // 남은 토큰 수량 Redis에서 조회
    long getRemainingTokens(String estateId);

    // 토큰 수량 감소 (원자적 연산)
    Long decrementTokens(String estateId, long amount);

    // 토큰 수량 증가 (원자적 연산)
    Long incrementTokens(String estateId, long amount);

    // 매물 시세 조회
    RedisEstatePrice getRedisEstatePrice(Long estateId);

    Map<Long, RedisEstatePrice> getMultipleRedisEstatePrice(List<Long> estateIds);

    void deleteRedisEstatePrice(Long estateId);

}
