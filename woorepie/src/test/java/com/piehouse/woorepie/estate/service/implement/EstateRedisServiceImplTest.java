package com.piehouse.woorepie.estate.service.implement;

import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Dividend;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.repository.DividendRepository;
import com.piehouse.woorepie.estate.repository.EstatePriceRepository;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("EstateRedisServiceImpl 단위테스트")
class EstateRedisServiceImplTest {

    @Mock RedisTemplate<String, String> redisStringTemplate;
    @Mock RedisTemplate<String, Object> redisObjectTemplate;
    @Mock EstateRepository estateRepository;
    @Mock EstatePriceRepository estatePriceRepository;
    @Mock DividendRepository dividendRepository;

    @InjectMocks
    EstateRedisServiceImpl estateRedisService;

    ValueOperations<String, String> mockStringOps;
    ValueOperations<String, Object> mockObjectOps;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockStringOps = mock(ValueOperations.class);
        mockObjectOps = mock(ValueOperations.class);

        when(redisStringTemplate.opsForValue()).thenReturn(mockStringOps);
        when(redisObjectTemplate.opsForValue()).thenReturn(mockObjectOps);
    }

    /**
     * [정상 케이스] 청약 초기화 - DB에서 토큰 개수 읽고 Redis에 저장
     */
    @Test
    @DisplayName("initializeRemainingTokens - DB에서 토큰 개수 읽고 Redis에 저장")
    void initializeRemainingTokens_success() {
        Long estateId = 123L;
        Integer tokenAmount = 100;

        when(estateRepository.findTokenAmountByEstateId(estateId)).thenReturn(Optional.of(tokenAmount));
        doNothing().when(mockStringOps).set(anyString(), eq(tokenAmount.toString()));

        // when
        estateRedisService.initializeRemainingTokens(estateId);

        // then
        verify(mockStringOps).set(contains(estateId.toString()), eq("100"));
    }

    /**
     * [예외 케이스] 청약 초기화 - DB에 estateId 없음
     */
    @Test
    @DisplayName("initializeRemainingTokens - estateId 미존재 예외")
    void initializeRemainingTokens_noEstate_fail() {
        Long estateId = 999L;
        when(estateRepository.findTokenAmountByEstateId(estateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estateRedisService.initializeRemainingTokens(estateId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ESTATE_NOT_FOUND.getMessage());
    }

    /**
     * [정상 케이스] setRemainingTokens/getRemainingTokens - 남은 토큰 수 저장/조회
     */
    @Test
    @DisplayName("setRemainingTokens/getRemainingTokens - 남은 토큰 수 저장/조회")
    void setAndGetRemainingTokens_success() {
        String estateId = "456";
        int remain = 77;

        doNothing().when(mockStringOps).set(anyString(), eq(String.valueOf(remain)));
        when(mockStringOps.get(anyString())).thenReturn(String.valueOf(remain));

        estateRedisService.setRemainingTokens(estateId, remain);

        int actual = estateRedisService.getRemainingTokens(estateId);
        assertThat(actual).isEqualTo(remain);

        verify(mockStringOps).set(contains(estateId), eq(String.valueOf(remain)));
        verify(mockStringOps).get(contains(estateId));
    }

    /**
     * [정상 케이스] decrementTokens/incrementTokens - 토큰 수량 증감
     */
    @Test
    @DisplayName("decrementTokens/incrementTokens - 토큰 수량 증감")
    void decrementAndIncrementTokens_success() {
        String estateId = "789";
        String key = "subscription:" + estateId + ":remaining-tokens";

        when(mockStringOps.decrement(eq(key), eq(5L))).thenReturn(10L);
        when(mockStringOps.increment(eq(key), eq(3L))).thenReturn(13L);

        Long afterDec = estateRedisService.decrementTokens(estateId, 5);
        Long afterInc = estateRedisService.incrementTokens(estateId, 3);

        assertThat(afterDec).isEqualTo(10L);
        assertThat(afterInc).isEqualTo(13L);
    }

    /**
     * [정상 케이스] getRedisEstatePrice - Redis 캐시 miss → DB에서 조회 및 Redis 저장
     */
    @Test
    @DisplayName("getRedisEstatePrice - Redis miss → DB 조회 및 Redis 저장")
    void getRedisEstatePrice_cacheMiss_success() {
        Long estateId = 1L;
        String key = "estate:price:" + estateId;
        Estate estate = mock(Estate.class);

        // 캐시 미스
        when(mockObjectOps.get(key)).thenReturn(null);
        when(estateRepository.findById(estateId)).thenReturn(Optional.of(estate));
        when(estate.getEstateSalePrice()).thenReturn(10000);
        when(estate.getTokenAmount()).thenReturn(100);
        when(dividendRepository.findTopByEstate_EstateIdOrderByDividendDateDesc(estateId)).thenReturn(Optional.empty());

        // mock set
        doNothing().when(mockObjectOps).set(anyString(), any(RedisEstatePrice.class), anyLong(), any(TimeUnit.class));

        // when
        RedisEstatePrice result = estateRedisService.getRedisEstatePrice(estateId);

        // then
        assertThat(result.getEstatePrice()).isEqualTo(10000);
        assertThat(result.getEstateTokenPrice()).isEqualTo(100);
        assertThat(result.getTokenAmount()).isEqualTo(100);
        assertThat(result.getDividendYield()).isEqualTo(BigDecimal.ZERO);
        verify(mockObjectOps).set(eq(key), any(RedisEstatePrice.class), eq(7L), eq(TimeUnit.DAYS));
    }

    /**
     * [정상 케이스] getRedisEstatePrice - Redis 캐시 hit
     */
    @Test
    @DisplayName("getRedisEstatePrice - Redis 캐시 hit")
    void getRedisEstatePrice_cacheHit_success() {
        Long estateId = 2L;
        String key = "estate:price:" + estateId;
        RedisEstatePrice price = RedisEstatePrice.builder()
                .estatePrice(11111)
                .estateTokenPrice(222)
                .tokenAmount(333)
                .dividendYield(BigDecimal.TEN)
                .build();

        when(mockObjectOps.get(key)).thenReturn(price);

        // when
        RedisEstatePrice result = estateRedisService.getRedisEstatePrice(estateId);

        // then
        assertThat(result).isEqualTo(price);
    }

    /**
     * [정상 케이스] getMultipleRedisEstatePrice - 일부 캐시 HIT, 일부 MISS
     */
    @Test
    @DisplayName("getMultipleRedisEstatePrice - 일부 캐시 HIT, 일부 MISS")
    void getMultipleRedisEstatePrice_mixedCache() {
        Long id1 = 10L, id2 = 20L;
        String key1 = "estate:price:" + id1;
        String key2 = "estate:price:" + id2;
        RedisEstatePrice cached = RedisEstatePrice.builder().estatePrice(100).build();

        // 첫 번째는 캐시 hit, 두 번째는 miss
        when(mockObjectOps.multiGet(List.of(key1, key2))).thenReturn(List.of(cached, null));
        when(mockObjectOps.get(key2)).thenReturn(null); // 두번째 miss
        Estate estate = mock(Estate.class);
        when(estateRepository.findById(id2)).thenReturn(Optional.of(estate));
        when(estate.getEstateSalePrice()).thenReturn(200);
        when(estate.getTokenAmount()).thenReturn(2);
        when(dividendRepository.findTopByEstate_EstateIdOrderByDividendDateDesc(id2)).thenReturn(Optional.empty());
        doNothing().when(mockObjectOps).set(anyString(), any(RedisEstatePrice.class), anyLong(), any(TimeUnit.class));

        // when
        Map<Long, RedisEstatePrice> result = estateRedisService.getMultipleRedisEstatePrice(List.of(id1, id2));

        // then
        assertThat(result.get(id1)).isEqualTo(cached);
        assertThat(result.get(id2).getEstatePrice()).isEqualTo(200);
    }

    /**
     * [예외 케이스] getRedisEstatePrice - 매물 미존재 시 예외
     */
    @Test
    @DisplayName("getRedisEstatePrice - 매물 미존재 시 예외")
    void getRedisEstatePrice_noEstate_fail() {
        Long estateId = 123L;
        String key = "estate:price:" + estateId;
        when(mockObjectOps.get(key)).thenReturn(null);
        when(estateRepository.findById(estateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estateRedisService.getRedisEstatePrice(estateId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ESTATE_NOT_FOUND.getMessage());
    }

    /**
     * [정상 케이스] deleteRedisEstatePrice - Redis에서 키 삭제
     */
    @Test
    @DisplayName("deleteRedisEstatePrice - Redis에서 키 삭제")
    void deleteRedisEstatePrice_success() {
        Long estateId = 456L;
        String key = "estate:price:" + estateId;
        doNothing().when(redisStringTemplate).delete(key);

        estateRedisService.deleteRedisEstatePrice(estateId);

        verify(redisStringTemplate).delete(key);
    }
}
