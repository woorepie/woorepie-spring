package com.piehouse.woorepie.estate.service.implement;

import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Dividend;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstatePrice;
import com.piehouse.woorepie.estate.repository.DividendRepository;
import com.piehouse.woorepie.estate.repository.EstatePriceRepository;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.estate.service.EstateRedisService;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class EstateRedisServiceImpl implements EstateRedisService {

    private final RedisTemplate<String, String> redisStringTemplate;
    private final RedisTemplate<String, Object> redisObjectTemplate;
    private final EstateRepository estateRepository;
    private final EstatePriceRepository estatePriceRepository;
    private final DividendRepository dividendRepository;
    private static final String REDIS_ESTATE_PRICE_KEY_PREFIX = "estate:price:";

    private static final String REMAINING_TOKENS_KEY_FORMAT = "subscription:%s:remaining-tokens"; // estateId

    // 청약 오픈 시 PostgreSQL에서 tokenAmount를 읽어와 Redis에 초기화
    @Transactional(readOnly = true)
    public void initializeRemainingTokens(Long estateId) {
        // 1. DB에서 tokenAmount 조회
        Long tokenAmount = estateRepository.findTokenAmountByEstateId(estateId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESTATE_NOT_FOUND));

        // 2. Redis에 저장 (초기화)
        String key = String.format(REMAINING_TOKENS_KEY_FORMAT, estateId);
        redisStringTemplate.opsForValue().set(key, String.valueOf(tokenAmount));
    }

    // 남은 토큰 수량 Reids에 저장 (STRING)
    public void setRemainingTokens(String estateId, long remainingTokens) {
        String key = String.format(REMAINING_TOKENS_KEY_FORMAT, estateId);
        redisStringTemplate.opsForValue().set(key, String.valueOf(remainingTokens));
    }

    // 남은 토큰 수량 Redis에서 조회
    public long getRemainingTokens(String estateId) {
        String key = String.format(REMAINING_TOKENS_KEY_FORMAT, estateId);
        String value = redisStringTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    // 토큰 수량 감소 (원자적 연산)
    public Long decrementTokens(String estateId, long amount) {
        String key = String.format(REMAINING_TOKENS_KEY_FORMAT, estateId);
        return redisStringTemplate.opsForValue().decrement(key, amount);
    }

    // 토큰 수량 증가 (원자적 연산)
    public Long incrementTokens(String estateId, long amount) {
        String key = String.format(REMAINING_TOKENS_KEY_FORMAT, estateId);
        return redisStringTemplate.opsForValue().increment(key, amount);
    }

    // 레디스에서 매물 시세 정보 가져오기
    @Override
    @Transactional
    public RedisEstatePrice getRedisEstatePrice(Long estateId) {
        String key = REDIS_ESTATE_PRICE_KEY_PREFIX + estateId;
        ValueOperations<String, Object> ops = redisObjectTemplate.opsForValue();

        Object cached = ops.get(key);
        if (cached instanceof RedisEstatePrice price) {
            return price;
        }

        Estate estate = estateRepository.findById(estateId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESTATE_NOT_FOUND));

//        EstatePrice latest = estatePriceRepository
//                .findTopByEstate_EstateIdOrderByEstatePriceDateDesc(estateId)
//                .orElse(null);
        Long estateSalePrice = estate.getEstateSalePrice();

        long tokenCount = estate.getTokenAmount();
        long estatePrice = estateSalePrice != null ? estateSalePrice : 0;
        long estateTokenPrice = tokenCount != 0 ? estatePrice / tokenCount : 0;

        // 가장 최근 배당금
        BigDecimal dividendYield = dividendRepository
                .findTopByEstate_EstateIdOrderByDividendDateDesc(estateId)
                .map(Dividend::getDividendYield)
                .orElse(null);

        // Redis 저장 객체 생성
        RedisEstatePrice rep = RedisEstatePrice.builder()
                .estatePrice(estatePrice)
                .estateTokenPrice(estateTokenPrice)
                .tokenAmount(tokenCount)
                .dividendYield(dividendYield != null ? dividendYield : BigDecimal.ZERO)
                .build();

        // Redis 캐싱 후 반환
        ops.set(key, rep, 7, TimeUnit.DAYS);
        return rep;

    }

    // 레드스에서 매물 시세 정보 Redis Bulk 조회
    @Override
    public Map<Long, RedisEstatePrice> getMultipleRedisEstatePrice(List<Long> estateIds) {

        if (estateIds == null || estateIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> keys = estateIds.stream()
                .map(id -> REDIS_ESTATE_PRICE_KEY_PREFIX + id)
                .toList();

        List<Object> cachedList = redisObjectTemplate.opsForValue().multiGet(keys);

        Map<Long, RedisEstatePrice> result = new HashMap<>();
        for (int i = 0; i < estateIds.size(); i++) {
            Object cached = cachedList.get(i);
            if (cached instanceof RedisEstatePrice price) {
                result.put(estateIds.get(i), price);
            } else {
                // 캐시 없으면 개별 estateId에 대해 기존 로직 수행 후 캐싱
                RedisEstatePrice rep = getRedisEstatePrice(estateIds.get(i));
                result.put(estateIds.get(i), rep);
            }
        }

        return result;
    }

    @Override
    public void deleteRedisEstatePrice(Long estateId) {
        redisStringTemplate.delete(REDIS_ESTATE_PRICE_KEY_PREFIX + estateId);
    }

}
