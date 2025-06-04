package com.piehouse.woorepie.trade.service.implement;

import com.piehouse.woorepie.customer.entity.Account;
import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.customer.repository.AccountRepository;
import com.piehouse.woorepie.customer.repository.CustomerRepository;
import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.global.kafka.dto.OrderCreatedEvent;
import com.piehouse.woorepie.global.kafka.dto.SubscriptionRequestEvent;
import com.piehouse.woorepie.global.kafka.dto.TransactionCreatedEvent;
import com.piehouse.woorepie.global.kafka.service.KafkaProducerService;
import com.piehouse.woorepie.notification.service.NotificationService;
import com.piehouse.woorepie.subscription.entity.SubStatus;
import com.piehouse.woorepie.subscription.entity.Subscription;
import com.piehouse.woorepie.subscription.repository.SubscriptionRepository;
import com.piehouse.woorepie.trade.dto.request.BuyEstateRequest;
import com.piehouse.woorepie.trade.dto.request.CreateSubscriptionTradeRequest;
import com.piehouse.woorepie.trade.dto.request.RedisCustomerTradeValue;
import com.piehouse.woorepie.trade.dto.request.RedisEstateTradeValue;
import com.piehouse.woorepie.trade.dto.request.SellEstateRequest;
import com.piehouse.woorepie.trade.entity.Trade;
import com.piehouse.woorepie.trade.repository.RedisTradeRepository;
import com.piehouse.woorepie.trade.repository.TradeRepository;
import com.piehouse.woorepie.estate.service.EstateRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TradeServiceImpl 단위테스트")
class TradeServiceImplTest {

    @Mock TradeRepository tradeRepository;
    @Mock AccountRepository accountRepository;
    @Mock CustomerRepository customerRepository;
    @Mock RedisTradeRepository redisOrderRepository;
    @Mock KafkaProducerService kafkaProducerService;
    @Mock NotificationService notificationService;
    @Mock EstateRepository estateRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock PlatformTransactionManager transactionManager;
    @Mock EstateRedisService estateRedisService;

    @InjectMocks
    private TradeServiceImpl tradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * [정상 케이스] 거래 내역 저장 및 알림/계좌 처리
     * - 거래 엔티티 저장 → 이벤트 발행 → 계좌, 잔액 처리까지 정상동작 검증
     */
    @Test
    @DisplayName("saveTrade - 거래 내역 저장 및 알림, 계좌 처리")
    void saveTrade_success() {
        Estate estate = mock(Estate.class);
        Customer seller = mock(Customer.class);
        Customer buyer = mock(Customer.class);
        Account sellerAccount = mock(Account.class);
        Account buyerAccount = mock(Account.class);
        Trade trade = mock(Trade.class);

        // ID, getter 등 mock 세팅
        when(seller.getCustomerId()).thenReturn(1L);
        when(buyer.getCustomerId()).thenReturn(2L);
        when(estate.getEstateId()).thenReturn(123L);
        when(estate.getEstateName()).thenReturn("부동산A");
        when(trade.getEstate()).thenReturn(estate);
        when(trade.getSeller()).thenReturn(seller);
        when(trade.getBuyer()).thenReturn(buyer);
        when(trade.getTokenPrice()).thenReturn(1000);
        when(trade.getTradeTokenAmount()).thenReturn(5);
        when(trade.getTradeId()).thenReturn(999L);
        when(trade.getTradeDate()).thenReturn(LocalDateTime.now());

        when(tradeRepository.save(any())).thenReturn(trade);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(customerRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(accountRepository.findByCustomerAndEstate(eq(seller), eq(estate))).thenReturn(Optional.of(sellerAccount));
        when(accountRepository.findByCustomerAndEstate(eq(buyer), eq(estate))).thenReturn(Optional.of(buyerAccount));
        when(buyerAccount.getAccountTokenAmount()).thenReturn(0);
        when(buyerAccount.getTotalAccountAmount()).thenReturn(0);
        when(sellerAccount.getAccountTokenAmount()).thenReturn(10);
        when(sellerAccount.getTotalAccountAmount()).thenReturn(10000);
        when(sellerAccount.updateTokenAmount(anyInt())).thenReturn(sellerAccount);
        when(sellerAccount.updateTotalAmount(anyInt())).thenReturn(sellerAccount);
        when(buyerAccount.updateTokenAmount(anyInt())).thenReturn(buyerAccount);
        when(buyerAccount.updateTotalAmount(anyInt())).thenReturn(buyerAccount);

        // when
        Trade result = tradeService.saveTrade(estate, seller, buyer, 5, 1000);

        // then
        assertThat(result).isNotNull();
        verify(tradeRepository).save(any());
        verify(kafkaProducerService).sendTransactionCreated(any(TransactionCreatedEvent.class));
        verify(notificationService, times(2)).sendTradeNotification(any(), anyString(), anyInt(), anyInt(), any(), anyBoolean());
        verify(sellerAccount).updateTokenAmount(5);
        verify(sellerAccount).updateTotalAmount(5000);
        verify(buyerAccount).updateTokenAmount(5);
        verify(buyerAccount).updateTotalAmount(5000);
        verify(seller).increaseAccountBalance(5000);
        verify(buyer).decreaseAccountBalance(5000);
    }

    /**
     * [예외 케이스] 거래 저장 - 판매자/구매자 미존재 시 예외
     * - 고객 조회 결과가 없을 때 예외 반환
     */
    @Test
    @DisplayName("saveTrade - 판매자/구매자 미존재 시 예외")
    void saveTrade_noUser_fail() {
        Estate estate = mock(Estate.class);
        Customer seller = mock(Customer.class);
        Customer buyer = mock(Customer.class);
        when(seller.getCustomerId()).thenReturn(1L);
        when(buyer.getCustomerId()).thenReturn(2L);
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        when(customerRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.saveTrade(estate, seller, buyer, 1, 1000))
                .isInstanceOf(CustomException.class);
    }

    /**
     * [정상 케이스] 매수 주문 요청 - 잔액 검증 및 Kafka 발행 정상동작
     */
    @Test
    @DisplayName("buy - 매수 요청 성공 및 Kafka 발행")
    void buy_success() {
        BuyEstateRequest request = mock(BuyEstateRequest.class);
        when(request.getTradeTokenAmount()).thenReturn(2);
        when(request.getTokenPrice()).thenReturn(1000);
        when(request.getEstateId()).thenReturn(10L);

        Customer customer = mock(Customer.class);
        when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(customer.getAccountBalance()).thenReturn(10000);

        when(redisOrderRepository.getCustomerBuyOrders(3L)).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> tradeService.buy(request, 3L));
        verify(kafkaProducerService).sendOrderCreated(any(OrderCreatedEvent.class));
    }

    /**
     * [예외 케이스] 매수 요청 실패 - 잔액 부족
     * - 요청금액이 잔액 초과 시 예외 발생
     */
    @Test
    @DisplayName("buy - 잔액 부족 예외")
    void buy_insufficientCash_fail() {
        BuyEstateRequest request = mock(BuyEstateRequest.class);
        when(request.getTradeTokenAmount()).thenReturn(10);
        when(request.getTokenPrice()).thenReturn(2000);
        when(request.getEstateId()).thenReturn(10L);

        Customer customer = mock(Customer.class);
        when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(customer.getAccountBalance()).thenReturn(1000);
        when(redisOrderRepository.getCustomerBuyOrders(3L)).thenReturn(List.of());

        assertThatThrownBy(() -> tradeService.buy(request, 3L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_CASH.getMessage());
    }

    /**
     * [정상 케이스] 매도 요청 성공 - 보유량 충분, Kafka 발행 정상
     */
    @Test
    @DisplayName("sell - 매도 요청 성공 및 Kafka 발행")
    void sell_success() {
        SellEstateRequest request = mock(SellEstateRequest.class);
        when(request.getEstateId()).thenReturn(10L);
        when(request.getTradeTokenAmount()).thenReturn(3);
        when(request.getTokenPrice()).thenReturn(800);

        Account account = mock(Account.class);
        when(account.getAccountTokenAmount()).thenReturn(10);

        when(accountRepository.findByCustomer_CustomerIdAndEstate_EstateId(3L, 10L)).thenReturn(Optional.of(account));
        when(redisOrderRepository.getEstateSellOrders(10L)).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> tradeService.sell(request, 3L));
        verify(kafkaProducerService).sendOrderCreated(any(OrderCreatedEvent.class));
    }

    /**
     * [예외 케이스] 매도 요청 실패 - 토큰 미보유
     * - 계좌가 존재하지 않으면 예외 발생
     */
    @Test
    @DisplayName("sell - 토큰 미보유 예외")
    void sell_tokenNotExist_fail() {
        SellEstateRequest request = mock(SellEstateRequest.class);
        when(request.getEstateId()).thenReturn(10L);
        when(request.getTradeTokenAmount()).thenReturn(5);
        when(accountRepository.findByCustomer_CustomerIdAndEstate_EstateId(3L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tradeService.sell(request, 3L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.TOKEN_NON_EXIST.getMessage());
    }

    /**
     * [정상 케이스] 청약 신청 - 잔액 충분, 기간 내 정상 Kafka 발송
     */
    @Test
    @DisplayName("createSubscription - 정상 청약 요청")
    void createSubscription_success() {
        CreateSubscriptionTradeRequest request = mock(CreateSubscriptionTradeRequest.class);
        when(request.getEstateId()).thenReturn(15L);
        when(request.getSubAmount()).thenReturn(3);

        Customer customer = mock(Customer.class);
        when(customer.getAccountBalance()).thenReturn(10000);
        when(customerRepository.findById(99L)).thenReturn(Optional.of(customer));

        Estate estate = mock(Estate.class);
        when(estateRepository.findById(15L)).thenReturn(Optional.of(estate));
        when(estate.getSubStartDate()).thenReturn(LocalDateTime.now().minusDays(1));
        when(estate.getSubEndDate()).thenReturn(LocalDateTime.now().plusDays(1));

        RedisEstatePrice price = mock(RedisEstatePrice.class);
        when(price.getEstateTokenPrice()).thenReturn(2000);
        when(estateRedisService.getRedisEstatePrice(15L)).thenReturn(price);

        when(redisOrderRepository.getCustomerBuyOrders(99L)).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> tradeService.createSubscription(request, 99L));
        verify(kafkaProducerService).sendSubscriptionRequest(any(SubscriptionRequestEvent.class));
    }

    /**
     * [예외 케이스] 청약 신청 실패 - 기간 외 예외
     * - 청약 기간 외일 때 예외 발생
     */
    @Test
    @DisplayName("createSubscription - 청약 기간 외 예외")
    void createSubscription_outOfPeriod_fail() {
        CreateSubscriptionTradeRequest request = mock(CreateSubscriptionTradeRequest.class);
        when(request.getEstateId()).thenReturn(100L);
        when(request.getSubAmount()).thenReturn(2);

        Customer customer = mock(Customer.class);
        when(customer.getAccountBalance()).thenReturn(10000);
        when(customerRepository.findById(99L)).thenReturn(Optional.of(customer));

        Estate estate = mock(Estate.class);
        when(estateRepository.findById(100L)).thenReturn(Optional.of(estate));
        when(estate.getSubStartDate()).thenReturn(LocalDateTime.now().plusDays(2)); // 아직 시작 전
        when(estate.getSubEndDate()).thenReturn(LocalDateTime.now().plusDays(10));

        assertThatThrownBy(() -> tradeService.createSubscription(request, 99L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.SUBSCRIPTION_PERIOD_INVALID.getMessage());
    }

    /**
     * [정상 케이스] 청약 요청 처리 - RUNNING 상태, 잔액 차감 및 저장 정상
     */
    @Test
    @DisplayName("processSubscriptionRequest - RUNNING 상태에서 청약 처리")
    void processSubscriptionRequest_success() {
        Estate estate = mock(Estate.class);
        when(estateRepository.findById(111L)).thenReturn(Optional.of(estate));
        when(estate.getEstateStatus()).thenReturn(EstateStatus.RUNNING);

        Customer customer = mock(Customer.class);
        when(customerRepository.findById(11L)).thenReturn(Optional.of(customer));
        when(customerRepository.decreaseBalance(eq(11L), anyInt())).thenReturn(1);

        assertThatNoException().isThrownBy(() ->
                tradeService.processSubscriptionRequest(111L, 11L, 2, 1000)
        );
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    /**
     * [예외 케이스] 청약 요청 처리 실패 - 매물 상태가 RUNNING 아님
     * - READY 등 다른 상태면 예외 발생
     */
    @Test
    @DisplayName("processSubscriptionRequest - 상태 비정상 예외")
    void processSubscriptionRequest_invalidStatus_fail() {
        Estate estate = mock(Estate.class);
        when(estateRepository.findById(222L)).thenReturn(Optional.of(estate));
        when(estate.getEstateStatus()).thenReturn(EstateStatus.READY);

        assertThatThrownBy(() -> tradeService.processSubscriptionRequest(222L, 33L, 2, 1000))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ESTATE_NOT_RUNNING.getMessage());
    }
}
