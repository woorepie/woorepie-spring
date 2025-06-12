package com.piehouse.woorepie.subscription.service.implement;

import com.piehouse.woorepie.agent.entity.Agent;
import com.piehouse.woorepie.agent.repository.AgentRepository;
import com.piehouse.woorepie.customer.entity.Account;
import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.customer.repository.AccountRepository;
import com.piehouse.woorepie.customer.repository.CustomerRepository;
import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.estate.service.implement.EstateRedisServiceImpl;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.global.kafka.service.KafkaProducerService;
import com.piehouse.woorepie.global.service.implement.S3ServiceImpl;
import com.piehouse.woorepie.notification.service.NotificationService;
import com.piehouse.woorepie.subscription.dto.request.RegisterEstateRequest;
import com.piehouse.woorepie.subscription.dto.response.GetSubscriptionDetailsResponse;
import com.piehouse.woorepie.subscription.dto.response.GetSubscriptionSimpleResponse;
import com.piehouse.woorepie.subscription.entity.SubStatus;
import com.piehouse.woorepie.subscription.entity.Subscription;
import com.piehouse.woorepie.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

class SubscriptionServiceImplTest {

    @Mock private EstateRepository estateRepository;
    @Mock private AgentRepository agentRepository;
    @Mock private EstateRedisServiceImpl estateRedisServiceImpl;
    @Mock private S3ServiceImpl s3serviceImpl;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private KafkaProducerService kafkaProducerService;
    @Mock private CustomerRepository customerRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("매물 등록 성공")
    void registerEstate_success() {
        Long agentId = 1L;
        RegisterEstateRequest request = mock(RegisterEstateRequest.class);
        Agent agent = mock(Agent.class);

        given(agent.getAgentEmail()).willReturn("woori@woori.com");
        given(agentRepository.findById(agentId)).willReturn(Optional.of(agent));
        given(s3serviceImpl.getPublicS3Url(any())).willReturn("http://img.com/file.png");

        assertThatNoException().isThrownBy(() -> subscriptionService.registerEstate(request, agentId));
        then(estateRepository).should(times(1)).save(any(Estate.class));
    }

    @Test
    @DisplayName("매물 등록 실패 - Agent 미존재")
    void registerEstate_fail_noAgent() {
        Long agentId = 99L;
        RegisterEstateRequest request = mock(RegisterEstateRequest.class);
        given(agentRepository.findById(agentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.registerEstate(request, agentId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("청약 매물 리스트 정상 조회")
    void getActiveSubscriptions_success() {
        Estate estate = mock(Estate.class);
        Agent agent = mock(Agent.class);

        given(estate.getEstateId()).willReturn(1L);
        given(estate.getAgent()).willReturn(agent);
        given(agent.getAgentId()).willReturn(10L);
        given(agent.getAgentName()).willReturn("김중개");
        given(agent.getBusinessName()).willReturn("우리공인");
        given(estate.getEstateName()).willReturn("서울아파트");
        given(estate.getEstateState()).willReturn("서울특별시");
        given(estate.getEstateCity()).willReturn("중구");
        given(estate.getSubStartDate()).willReturn(LocalDateTime.now());
        given(estate.getSubEndDate()).willReturn(LocalDateTime.now().plusDays(10));
        given(estate.getEstateImageUrl()).willReturn("http://img.com/estate.png");
        given(estate.getEstateStatus()).willReturn(EstateStatus.RUNNING);

        given(estateRepository.findByEstateStatusIn(anyList())).willReturn(List.of(estate));
        RedisEstatePrice price = mock(RedisEstatePrice.class);
        given(price.getTokenAmount()).willReturn(10L);
        given(price.getEstatePrice()).willReturn(1000000L);
        given(price.getEstateTokenPrice()).willReturn(100000L);
        given(price.getDividendYield()).willReturn(BigDecimal.valueOf(5.5));
        Map<Long, RedisEstatePrice> priceMap = Map.of(1L, price);
        given(estateRedisServiceImpl.getMultipleRedisEstatePrice(anyList())).willReturn(priceMap);

        List<GetSubscriptionSimpleResponse> result = subscriptionService.getActiveSubscriptions();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        then(estateRepository).should(times(1)).findByEstateStatusIn(anyList());
        then(estateRedisServiceImpl).should(times(1)).getMultipleRedisEstatePrice(anyList());
    }

    @Test
    @DisplayName("청약 매물 리스트 - 데이터 없음(empty)")
    void getActiveSubscriptions_empty() {
        given(estateRepository.findByEstateStatusIn(anyList())).willReturn(List.of());
        given(estateRedisServiceImpl.getMultipleRedisEstatePrice(anyList())).willReturn(Map.of());

        List<GetSubscriptionSimpleResponse> result = subscriptionService.getActiveSubscriptions();

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("청약 매물 상세 조회 성공")
    void getSubscriptionDetails_success() {
        Long estateId = 10L;
        Estate estate = mock(Estate.class);
        Agent agent = mock(Agent.class);

        given(estateRepository.findById(estateId)).willReturn(Optional.of(estate));
        given(estateRedisServiceImpl.getRedisEstatePrice(estateId)).willReturn(mock(RedisEstatePrice.class));
        given(estate.getAgent()).willReturn(agent);
        given(agent.getBusinessName()).willReturn("우리공인");
        given(agent.getAgentId()).willReturn(11L);
        given(agent.getAgentName()).willReturn("김중개");
        given(estate.getEstateName()).willReturn("강남빌라");
        given(estate.getTokenAmount()).willReturn(10L);

        GetSubscriptionDetailsResponse response = subscriptionService.getSubscriptionDetails(estateId);

        assertThat(response).isNotNull();
        then(estateRepository).should(times(1)).findById(estateId);
    }

    @Test
    @DisplayName("청약 매물 상세 조회 실패 - estate 미존재")
    void getSubscriptionDetails_fail_noEstate() {
        Long estateId = 1234L;
        given(estateRepository.findById(estateId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscriptionDetails(estateId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ESTATE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("청약 성공 로직 - 전체 성공/부분 성공/실패")
    void updateSubscriptionsOnSuccess_logic() {
        Long estateId = 1L;
        Estate estate = mock(Estate.class);

        given(estateRepository.findById(estateId)).willReturn(Optional.of(estate));
        given(estate.getTokenAmount()).willReturn(5L);

        RedisEstatePrice redisPrice = mock(RedisEstatePrice.class);
        given(redisPrice.getEstateTokenPrice()).willReturn(1000L);
        given(estateRedisServiceImpl.getRedisEstatePrice(estateId)).willReturn(redisPrice);

        // 두 명의 고객이 각각 3개, 4개씩 신청 (총 5개만 가능)
        Customer customer1 = mock(Customer.class);
        Customer customer2 = mock(Customer.class);
        given(customer1.getCustomerId()).willReturn(11L);
        given(customer2.getCustomerId()).willReturn(12L);

        Subscription sub1 = mock(Subscription.class);
        Subscription sub2 = mock(Subscription.class);

        given(sub1.getSubTokenAmount()).willReturn(3L);
        given(sub2.getSubTokenAmount()).willReturn(4L);
        given(sub1.getCustomer()).willReturn(customer1);
        given(sub2.getCustomer()).willReturn(customer2);
        given(sub1.getEstate()).willReturn(estate);
        given(sub2.getEstate()).willReturn(estate);
        given(sub1.getSubDate()).willReturn(LocalDateTime.now());
        given(sub2.getSubDate()).willReturn(LocalDateTime.now());
        given(sub1.getSubStatus()).willReturn(SubStatus.PENDING);
        given(sub2.getSubStatus()).willReturn(SubStatus.PENDING);

        // accountRepository, Account mock (Optional 타입 주의!)
        Account account1 = mock(Account.class);
        given(accountRepository.findByCustomerAndEstate(eq(customer1), eq(estate))).willReturn(Optional.of(account1));
        given(accountRepository.findByCustomerAndEstate(eq(customer2), eq(estate))).willReturn(Optional.empty());

        // **이 부분이 중요!**
        given(customerRepository.increaseBalance(anyLong(), anyInt())).willReturn(1);

        // 알림 mock
        willDoNothing().given(notificationService).sendSubscriptionSuccessNotification(any(), any(), anyInt(), anyInt(), any());
        willDoNothing().given(notificationService).sendSubscriptionFailSoldoutNotification(any(), any(), anyInt(), anyInt(), any());

        // 선착순 배정 Pending list
        given(subscriptionRepository.findAllByEstate_EstateIdAndSubStatusOrderBySubDateAsc(estateId, SubStatus.PENDING))
                .willReturn(List.of(sub1, sub2));

        assertThatNoException().isThrownBy(() -> subscriptionService.updateSubscriptionsOnSuccess(estateId));
        then(estateRepository).should(atLeastOnce()).save(any(Estate.class));
    }

    @Test
    @DisplayName("청약 실패 처리 - 전체 pending 실패")
    void updateSubscriptionsOnFailure_success() {
        Long estateId = 1L;
        Estate estate = mock(Estate.class);

        Customer customer = mock(Customer.class);
        given(customer.getCustomerId()).willReturn(100L);

        Subscription sub1 = mock(Subscription.class);
        given(sub1.getCustomer()).willReturn(customer);
        given(sub1.getEstate()).willReturn(estate);
        given(sub1.getSubTokenAmount()).willReturn(2L);
        given(sub1.getSubDate()).willReturn(LocalDateTime.now());
        Subscription sub2 = mock(Subscription.class);
        given(sub2.getCustomer()).willReturn(customer);
        given(sub2.getEstate()).willReturn(estate);
        given(sub2.getSubTokenAmount()).willReturn(3L);
        given(sub2.getSubDate()).willReturn(LocalDateTime.now());

        List<Subscription> pendingList = List.of(sub1, sub2);

        given(subscriptionRepository.findAllByEstate_EstateIdAndSubStatus(estateId, SubStatus.PENDING))
                .willReturn(pendingList);
        RedisEstatePrice price = mock(RedisEstatePrice.class);
        given(price.getEstateTokenPrice()).willReturn(500L);
        given(estateRedisServiceImpl.getRedisEstatePrice(estateId)).willReturn(price);
        given(estateRepository.findById(estateId)).willReturn(Optional.of(estate));

        // **이 부분이 중요!**
        given(customerRepository.increaseBalance(anyLong(), anyInt())).willReturn(1);

        willDoNothing().given(notificationService).sendSubscriptionFailLackNotification(any(), any(), anyInt(), anyInt(), any());

        assertThatNoException().isThrownBy(() -> subscriptionService.updateSubscriptionsOnFailure(estateId));
        then(subscriptionRepository).should().saveAll(pendingList);
        then(estateRepository).should().save(estate);
    }

    @Test
    @DisplayName("청약 실패 환불 - 예외 발생")
    void refundSubscriptionFailure_fail() {
        Subscription sub = mock(Subscription.class);
        Customer customer = mock(Customer.class);
        given(sub.getCustomer()).willReturn(customer);
        given(customer.getCustomerId()).willReturn(123L);
        given(sub.getSubTokenAmount()).willReturn(2L);
        given(customerRepository.increaseBalance(eq(123L), anyInt())).willReturn(0);

        assertThatThrownBy(() -> subscriptionService.refundSubscriptionFailure(sub, 1000))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ACCOUNT_NON_EXIST.getMessage());
    }
}
