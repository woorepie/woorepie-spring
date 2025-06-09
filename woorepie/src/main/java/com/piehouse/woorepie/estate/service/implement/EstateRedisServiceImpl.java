package com.piehouse.woorepie.estate.service.implement;

import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Dividend;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.repository.DividendRepository;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.estate.service.EstateRedisService;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    private final DividendRepository dividendRepository;
    private static final String REDIS_ESTATE_PRICE_KEY_PREFIX = "estate:price:";
    private static final String REMAINING_TOKENS_KEY_FORMAT = "estate:%s:remainingTokens";


    private DefaultRedisScript<Long> decrementScript; // Lua Script용 필드

    // Lua Script 초기화
    @PostConstruct
    public void initDecrementScript() {
        decrementScript = new DefaultRedisScript<>();
        decrementScript.setLocation(new ClassPathResource("scripts/decrement_if_possible.lua"));
        decrementScript.setResultType(Long.class);
    }

    // 남은 토큰 수량 Redis에서 조회 및 초기화
    @Override
    public Long getRemainingTokensOrInit(Long estateId) {
        String key = String.format(REMAINING_TOKENS_KEY_FORMAT, estateId);
        String value = redisStringTemplate.opsForValue().get(key);
        if (value != null) return Long.parseLong(value);

        Long tokenAmount = estateRepository.findTokenAmountByEstateId(estateId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESTATE_NOT_FOUND));
        redisStringTemplate.opsForValue().setIfAbsent(key, String.valueOf(tokenAmount));
        return tokenAmount;
    }

    // Lua Script 기반 감소 메서드
    @Override
    public Long decrementButNotNegative(Long estateId, long amount) {
        String key = String.format(REMAINING_TOKENS_KEY_FORMAT, estateId);
        return redisStringTemplate.execute(
                decrementScript,
                Collections.singletonList(key),
                String.valueOf(amount)
        );
    }

    // 토큰 수량 증가 (원자적 연산)
    @Override
    public Long incrementTokens(Long estateId, long amount) {
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
