package com.piehouse.woorepie.trade.service.implement;

import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.customer.repository.CustomerRepository;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.global.kafka.dto.OrderCreatedEvent;
import com.piehouse.woorepie.notification.service.NotificationService;
import com.piehouse.woorepie.trade.dto.request.RedisCustomerTradeValue;
import com.piehouse.woorepie.trade.dto.request.RedisEstateTradeValue;
import com.piehouse.woorepie.trade.repository.RedisTradeRepository;
import com.piehouse.woorepie.trade.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TradeRedisServiceImpl 단위테스트")
class TradeRedisServiceImplTest {

    @Mock private RedisTradeRepository redisRepository;
    @Mock private TradeService tradeService;
    @Mock private EstateRepository estateRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private TradeRedisServiceImpl tradeRedisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * [정상 케이스] 매수 주문 저장 기능
     * - Lua 스크립트를 이용한 원자적 저장 동작 여부 검증
     */
    @Test
    @DisplayName("매수 주문 저장 - Lua 스크립트 원자적 저장")
    void saveBuyOrder_success() {
        doNothing().when(redisRepository).saveOrUpdateBuyOrder(any(), anyLong(), any(), anyLong());

        assertThatNoException().isThrownBy(() ->
                tradeRedisService.saveBuyOrder(1L, 2L, 10, 1000)
        );
        verify(redisRepository).saveOrUpdateBuyOrder(any(), eq(1L), any(), eq(2L));
    }

    /**
     * [정상 케이스] 매도 주문 저장 기능
     * - Lua 스크립트를 이용한 원자적 저장 동작 여부 검증
     */
    @Test
    @DisplayName("매도 주문 저장 - Lua 스크립트 원자적 저장")
    void saveSellOrder_success() {
        doNothing().when(redisRepository).saveOrUpdateSellOrder(any(), anyLong(), any(), anyLong());

        assertThatNoException().isThrownBy(() ->
                tradeRedisService.saveSellOrder(1L, 2L, 10, 1000)
        );
        verify(redisRepository).saveOrUpdateSellOrder(any(), eq(1L), any(), eq(2L));
    }

    /**
     * [정상 케이스] 매물 기준 매수/매도 주문 전체 조회
     * - 조회 결과가 올바르게 반환되는지 검증
     */
    @Test
    @DisplayName("매물 기준 매수/매도 주문 전체 조회")
    void getEstateOrders_success() {
        List<RedisEstateTradeValue> buyList = List.of(
                new RedisEstateTradeValue(1L, 10, 1000, 111L)
        );
        List<RedisEstateTradeValue> sellList = List.of(
                new RedisEstateTradeValue(2L, -5, 990, 112L)
        );
        when(redisRepository.getEstateBuyOrders(1L)).thenReturn(buyList);
        when(redisRepository.getEstateSellOrders(1L)).thenReturn(sellList);

        List<RedisEstateTradeValue> actualBuy = tradeRedisService.getEstateBuyOrders(1L);
        List<RedisEstateTradeValue> actualSell = tradeRedisService.getEstateSellOrders(1L);

        assertThat(actualBuy).isEqualTo(buyList);
        assertThat(actualSell).isEqualTo(sellList);
    }

    /**
     * [정상 케이스] 고객 기준 매수/매도 주문 전체 조회
     * - 고객 ID별 주문 조회 정상 동작 검증
     */
    @Test
    @DisplayName("고객 기준 매수/매도 주문 전체 조회")
    void getCustomerOrders_success() {
        List<RedisCustomerTradeValue> buyList = List.of(
                new RedisCustomerTradeValue(1L, 10, 1000, 111L)
        );
        List<RedisCustomerTradeValue> sellList = List.of(
                new RedisCustomerTradeValue(2L, -5, 990, 112L)
        );
        when(redisRepository.getCustomerBuyOrders(2L)).thenReturn(buyList);
        when(redisRepository.getCustomerSellOrders(2L)).thenReturn(sellList);

        List<RedisCustomerTradeValue> actualBuy = tradeRedisService.getCustomerBuyOrders(2L);
        List<RedisCustomerTradeValue> actualSell = tradeRedisService.getCustomerSellOrders(2L);

        assertThat(actualBuy).isEqualTo(buyList);
        assertThat(actualSell).isEqualTo(sellList);
    }

    /**
     * [정상 케이스] 가장 오래된 매수/매도 주문 pop 동작 검증
     * - pop 결과가 정상 반환되는지 확인
     */
    @Test
    @DisplayName("가장 오래된 매수/매도 주문 pop")
    void popOldestOrders_success() {
        RedisEstateTradeValue buy = new RedisEstateTradeValue(1L, 10, 1000, 111L);
        RedisEstateTradeValue sell = new RedisEstateTradeValue(2L, -5, 990, 112L);
        when(redisRepository.popOldestBuyOrderFromBoth(1L)).thenReturn(buy);
        when(redisRepository.popOldestSellOrderFromBoth(1L)).thenReturn(sell);

        RedisEstateTradeValue actualBuy = tradeRedisService.popOldestBuyOrderFromBoth(1L);
        RedisEstateTradeValue actualSell = tradeRedisService.popOldestSellOrderFromBoth(1L);

        assertThat(actualBuy).isEqualTo(buy);
        assertThat(actualSell).isEqualTo(sell);
    }

    /**
     * [정상 케이스] 매수 주문 이벤트 매칭
     * - 주문 저장 및 분산 락 동작, 매칭 함수 실행 검증
     */
    @Test
    @DisplayName("매수 주문 이벤트 매칭 - 저장 및 락 동작")
    void matchNewBuyOrder_success() throws InterruptedException {
        doNothing().when(redisRepository).saveOrUpdateBuyOrder(any(), anyLong(), any(), anyLong());
        when(redissonClient.getFairLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // 매칭 시 pop된 주문이 없다는 상황을 가정
        when(redisRepository.popOldestBuyOrderFromBoth(anyLong())).thenReturn(null);
        when(redisRepository.popOldestSellOrderFromBoth(anyLong())).thenReturn(null);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .estateId(1L).customerId(2L).tradeTokenAmount(10L).tokenPrice(1000L).build();

        assertThatNoException().isThrownBy(() ->
                tradeRedisService.matchNewBuyOrder(event)
        );
        verify(rLock).unlock();
    }

    /**
     * [정상 케이스] 매도 주문 이벤트 매칭
     * - 주문 저장 및 분산 락 동작, 매칭 함수 실행 검증
     */
    @Test
    @DisplayName("매도 주문 이벤트 매칭 - 저장 및 락 동작")
    void matchNewSellOrder_success() throws InterruptedException {
        doNothing().when(redisRepository).saveOrUpdateSellOrder(any(), anyLong(), any(), anyLong());
        when(redissonClient.getFairLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // 매칭 시 pop된 주문이 없다는 상황을 가정
        when(redisRepository.popOldestBuyOrderFromBoth(anyLong())).thenReturn(null);
        when(redisRepository.popOldestSellOrderFromBoth(anyLong())).thenReturn(null);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .estateId(1L).customerId(2L).tradeTokenAmount(10L).tokenPrice(1000L).build();

        assertThatNoException().isThrownBy(() ->
                tradeRedisService.matchNewSellOrder(event)
        );
        verify(rLock).unlock();
    }

    /**
     * [동시성 케이스] 분산 락 획득/해제
     * - 여러 스레드 환경에서 락 경쟁 및 처리 검증
     */
    @Test
    @DisplayName("분산 락 획득/해제 - 동시성 환경에서 경쟁 처리")
    void processMatchingWithLock_concurrent() throws InterruptedException {
        when(redissonClient.getFairLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(redisRepository.popOldestBuyOrderFromBoth(anyLong())).thenReturn(null);
        when(redisRepository.popOldestSellOrderFromBoth(anyLong())).thenReturn(null);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                tradeRedisService.processMatchingWithLock(1L);
                latch.countDown();
            });
        }
        latch.await();

        verify(rLock, atLeastOnce()).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        verify(rLock, atLeastOnce()).unlock();
    }

    /**
     * [비정상 케이스] 매칭 도중 매물/고객 미존재
     * - 체결 시 매물 or 고객 미존재로 예외 발생 검증
     */
    @Test
    @DisplayName("매칭 도중 매물 또는 고객 미존재 - 예외 발생")
    void saveTradeTransaction_fail() {
        // 매물 조회 실패 케이스
        when(redisRepository.popOldestBuyOrderFromBoth(anyLong()))
                .thenReturn(new RedisEstateTradeValue(1L, 10, 1000, 111L));
        when(redisRepository.popOldestSellOrderFromBoth(anyLong()))
                .thenReturn(new RedisEstateTradeValue(2L, -10, 1000, 112L));
        when(estateRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeRedisService.matchAllPossibleOrders(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ESTATE_NOT_FOUND.getMessage());
    }

    /**
     * [부분 체결] 부분 주문 재삽입
     * - pop한 주문이 부분 체결되어 원본 타임스탬프 유지되는지 검증
     */
    @Test
    @DisplayName("부분 체결 후 주문 재삽입 - 원본 타임스탬프 유지")
    void reinsertOrder_success() {
        RedisEstateTradeValue buyOrder = new RedisEstateTradeValue(2L, 8, 999, 111L);
        doNothing().when(redisRepository).saveOrUpdateBuyOrder(any(), anyLong(), any(), anyLong());

        assertThatNoException().isThrownBy(() -> tradeRedisService.reinsertBuyOrder(1L, buyOrder));
        verify(redisRepository).saveOrUpdateBuyOrder(any(), eq(1L), any(), eq(2L));
    }
}
