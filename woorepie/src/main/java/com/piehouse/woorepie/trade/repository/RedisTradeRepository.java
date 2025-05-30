package com.piehouse.woorepie.trade.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piehouse.woorepie.trade.dto.request.RedisCustomerTradeValue;
import com.piehouse.woorepie.trade.dto.request.RedisEstateTradeValue;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RedisTradeRepository {
    private final RedisTemplate<String, String> redisStringTemplate; // Lua 실행용
    private final RedisTemplate<String, RedisEstateTradeValue> redisEstateTradeTemplate; // 일반 CRUD용
    private final RedisTemplate<String, RedisCustomerTradeValue> redisCustomerTradeTemplate; // 일반 CRUD용
    private final ObjectMapper objectMapper;

    // 키 패턴 상수
    private static final String ESTATE_BUY_KEY = "estate:%d:buy";
    private static final String ESTATE_SELL_KEY = "estate:%d:sell";
    private static final String CUSTOMER_BUY_KEY = "customer:%d:buy";
    private static final String CUSTOMER_SELL_KEY = "customer:%d:sell";

    // Lua 스크립트 로드
    // 주문 저장
    private final DefaultRedisScript<Long> saveScript = new DefaultRedisScript<>() {{
        setLocation(new ClassPathResource("scripts/save_order.lua"));
        setResultType(Long.class);
    }};

    // 가장 먼저 들어온 주문 꺼냄
    private final DefaultRedisScript<String> popScript = new DefaultRedisScript<>() {{
        setLocation(new ClassPathResource("scripts/pop_order.lua"));
        setResultType(String.class);
    }};

    // 매수 주문 저장 및 부분 체결 업데이트
    public void saveOrUpdateBuyOrder(RedisEstateTradeValue estateOrder, Long estateId,
                                     RedisCustomerTradeValue customerOrder, Long customerId) {
        String estateKey = String.format(ESTATE_BUY_KEY, estateId);
        String customerKey = String.format(CUSTOMER_BUY_KEY, customerId);
        executeSaveScript(estateKey, customerKey, estateOrder, customerOrder);
    }

    // 매도 주문 저장 및 부분 체결 업데이트
    public void saveOrUpdateSellOrder(RedisEstateTradeValue estateOrder, Long estateId,
                                      RedisCustomerTradeValue customerOrder, Long customerId) {
        String estateKey = String.format(ESTATE_SELL_KEY, estateId);
        String customerKey = String.format(CUSTOMER_SELL_KEY, customerId);
        executeSaveScript(estateKey, customerKey, estateOrder, customerOrder);
    }

    // 주문 저장 (Lua 스크립트 사용)
    private void executeSaveScript(String estateKey, String customerKey,
                                   RedisEstateTradeValue estateOrder,
                                   RedisCustomerTradeValue customerOrder) {
        try {
            String estateJson = objectMapper.writeValueAsString(estateOrder);
            String customerJson = objectMapper.writeValueAsString(customerOrder);
            redisStringTemplate.execute(
                    saveScript,
                    List.of(estateKey, customerKey),
                    estateJson, customerJson, String.valueOf(estateOrder.getTimestamp())
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("직렬화 실패", e);
        }
    }

    // 매물별 매수 주문 조회 (시간순)
    public List<RedisEstateTradeValue> getEstateBuyOrders(Long estateId) {
        String key = String.format(ESTATE_BUY_KEY, estateId);
        Set<String> jsonSet = redisStringTemplate.opsForZSet().range(key, 0, -1);

        if (jsonSet == null) return List.of();

        return jsonSet.stream()
                .filter(s -> s != null && !s.isEmpty())
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, RedisEstateTradeValue.class);
                    } catch (Exception e) {
                        throw new RuntimeException("역직렬화 실패: " + json, e);
                    }
                })
                .collect(Collectors.toList());
    }

    // 매물별 매도 주문 조회 (시간순)
    public List<RedisEstateTradeValue> getEstateSellOrders(Long estateId) {
        String key = String.format(ESTATE_SELL_KEY, estateId);
        Set<String> jsonSet = redisStringTemplate.opsForZSet().range(key, 0, -1);

        if (jsonSet == null) return List.of();

        return jsonSet.stream()
                .filter(s -> s != null && !s.isEmpty())
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, RedisEstateTradeValue.class);
                    } catch (Exception e) {
                        throw new RuntimeException("역직렬화 실패: " + json, e);
                    }
                })
                .collect(Collectors.toList());
    }

    // 고객별 매수 주문 조회 (시간순)
    public List<RedisCustomerTradeValue> getCustomerBuyOrders(Long customerId) {
        String key = String.format(CUSTOMER_BUY_KEY, customerId);
        Set<String> jsonSet = redisStringTemplate.opsForZSet().range(key, 0, -1);

        if (jsonSet == null) return List.of();

        return jsonSet.stream()
                .filter(s -> s != null && !s.isEmpty())
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, RedisCustomerTradeValue.class); // DTO 변환
                    } catch (Exception e) {
                        throw new RuntimeException("역직렬화 실패: " + json, e);
                    }
                })
                .collect(Collectors.toList());
    }

    // 고객별 매도 주문 조회 (시간순)
    public List<RedisCustomerTradeValue> getCustomerSellOrders(Long customerId) {
        String key = String.format(CUSTOMER_SELL_KEY, customerId);
        Set<String> jsonSet = redisStringTemplate.opsForZSet().range(key, 0, -1);

        if (jsonSet == null) return List.of();

        return jsonSet.stream()
                .filter(s -> s != null && !s.isEmpty())
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, RedisCustomerTradeValue.class); // DTO 변환
                    } catch (Exception e) {
                        throw new RuntimeException("역직렬화 실패: " + json, e);
                    }
                })
                .collect(Collectors.toList());
    }

    // 매수 주문 Pop (Lua 스크립트 사용)
    public RedisEstateTradeValue popOldestBuyOrderFromBoth(Long estateId) {
        String estateKey = String.format(ESTATE_BUY_KEY, estateId);
        String customerKeyPattern = CUSTOMER_BUY_KEY.replace("%d", "%s");

        String result = redisStringTemplate.execute(
                popScript,
                List.of(estateKey, customerKeyPattern)
        );

        return deserializeOrder(result);
    }

    // 매도 주문 Pop (Lua 스크립트 사용)
    public RedisEstateTradeValue popOldestSellOrderFromBoth(Long estateId) {
        String estateKey = String.format(ESTATE_SELL_KEY, estateId);
        String customerKeyPattern = CUSTOMER_SELL_KEY.replace("%d", "%s");

        String result = redisStringTemplate.execute(
                popScript,
                List.of(estateKey, customerKeyPattern)
        );

        return deserializeOrder(result);
    }

    // JSON 역직렬화
    private RedisEstateTradeValue deserializeOrder(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, RedisEstateTradeValue.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize order", e);
        }
    }
}
