package com.piehouse.woorepie.customer.service.implement;

import com.piehouse.woorepie.customer.dto.request.LoginCustomerRequest;
import com.piehouse.woorepie.customer.dto.request.ModifyPassword;
import com.piehouse.woorepie.customer.dto.response.GetCustomerAccountResponse;
import com.piehouse.woorepie.customer.dto.response.GetCustomerResponse;
import com.piehouse.woorepie.customer.dto.response.GetCustomerSubscriptionResponse;
import com.piehouse.woorepie.customer.dto.response.GetCustomerTradeResponse;
import com.piehouse.woorepie.customer.entity.Account;
import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.customer.repository.AccountRepository;
import com.piehouse.woorepie.customer.repository.CustomerRepository;
import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.service.EstateRedisService;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.kafka.service.KafkaProducerService;
import com.piehouse.woorepie.global.service.implement.S3ServiceImpl;
import com.piehouse.woorepie.subscription.entity.Subscription;
import com.piehouse.woorepie.subscription.repository.SubscriptionRepository;
import com.piehouse.woorepie.trade.entity.Trade;
import com.piehouse.woorepie.trade.repository.TradeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private TradeRepository tradeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EstateRedisService estateRedisService;
    @Mock private KafkaProducerService kafkaProducerService;
    @Mock private S3ServiceImpl s3ServiceImpl;

    @InjectMocks private CustomerServiceImpl customerService;

    @Mock private HttpServletRequest httpServletRequest;
    @Mock private HttpSession httpSession;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("이메일 중복 확인 - 존재하지 않으면 true")
    void checkCustomerEmail_shouldReturnTrue_whenEmailNotExists() {
        when(customerRepository.existsByCustomerEmail("test@example.com")).thenReturn(false);

        Boolean result = customerService.checkCustomerEmail("test@example.com");

        assertTrue(result);
        verify(customerRepository).existsByCustomerEmail("test@example.com");
    }

    @Test
    @DisplayName("이메일 중복 확인 - 존재하면 예외")
    void checkCustomerEmail_shouldThrowException_whenEmailExists() {
        when(customerRepository.existsByCustomerEmail("test@example.com")).thenReturn(true);

        assertThrows(CustomException.class,
                () -> customerService.checkCustomerEmail("test@example.com"));
    }

    @Test
    @DisplayName("전화번호 중복 확인 - 존재하지 않으면 true")
    void checkCustomerPhoneNumber_shouldReturnTrue_whenPhoneNotExists() {
        when(customerRepository.existsByCustomerPhoneNumber("01012345678")).thenReturn(false);

        Boolean result = customerService.checkCustomerPhoneNumber("01012345678");

        assertTrue(result);
    }

    @Test
    @DisplayName("전화번호 중복 확인 - 존재하면 예외")
    void checkCustomerPhoneNumber_shouldThrowException_whenPhoneExists() {
        when(customerRepository.existsByCustomerPhoneNumber("01012345678")).thenReturn(true);

        assertThrows(CustomException.class,
                () -> customerService.checkCustomerPhoneNumber("01012345678"));
    }

    @Test
    @DisplayName("로그인 성공 - 세션 저장")
    void customerLogin_shouldSetSession_whenValid() {
        String email = "test@example.com";
        String password = "password";
        String phone = "01012345678";

        Customer customer = Customer.builder()
                .customerEmail(email)
                .customerPassword("encoded")
                .customerPhoneNumber(phone)
                .build();

        when(customerRepository.findByCustomerEmail(email)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches(password, "encoded")).thenReturn(true);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);

        customerService.customerLogin(new LoginCustomerRequest(email, password, phone), httpServletRequest);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void customerLogin_shouldThrowException_whenInvalidPassword() {
        Customer customer = Customer.builder()
                .customerEmail("test@example.com")
                .customerPassword("encoded")
                .customerPhoneNumber("01012345678")
                .build();

        when(customerRepository.findByCustomerEmail("test@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(CustomException.class,
                () -> customerService.customerLogin(
                        new LoginCustomerRequest("test@example.com", "wrong", "01012345678"), httpServletRequest));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void modifyCustomerPassword_shouldUpdatePassword_whenCorrectCurrentPassword() {
        Long id = 1L;
        Customer customer = Customer.builder().customerId(id).customerPassword("encodedOld").build();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("old", "encodedOld")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("encodedNew");

        customerService.modifyCustomerPassword(id, new ModifyPassword("old", "new"));

        verify(customerRepository).save(any());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void modifyCustomerPassword_shouldThrow_whenCurrentPasswordIncorrect() {
        Long id = 1L;
        Customer customer = Customer.builder().customerId(id).customerPassword("encodedOld").build();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong", "encodedOld")).thenReturn(false);

        assertThrows(CustomException.class,
                () -> customerService.modifyCustomerPassword(id, new ModifyPassword("wrong", "new")));
    }

    @Test
    @DisplayName("계좌 잔액 충전 성공")
    void plusCustomerAccountBalance_shouldIncreaseBalance() {
        Long id = 1L;
        Customer customer = Customer.builder().customerId(id).accountBalance(0L).build();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        customerService.plusCustomerAccountBalance(id, 1000L);

        assertEquals(1000, customer.getAccountBalance());
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("마이페이지 조회 - 총 토큰 보유액 계산")
    void getCustomer_shouldReturnCustomerResponse() {
        Long id = 1L;
        Customer customer = Customer.builder()
                .customerId(id)
                .customerName("홍길동")
                .customerEmail("test@example.com")
                .customerPhoneNumber("01012345678")
                .accountNumber("123-456")
                .customerAddress("서울")
                .accountBalance(1000L)
                .build();

        Estate estate = Estate.builder()
                .estateId(1L)
                .estateName("테스트 부동산")
                .build();

        Account account = Account.builder()
                .accountTokenAmount(10L)
                .customer(customer)
                .estate(estate) // estate 반드시 세팅!
                .build();

        RedisEstatePrice redisEstatePrice = RedisEstatePrice.builder()
                .estateTokenPrice(100L)
                .estatePrice(1000000L)
                .build();

        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(accountRepository.findByCustomerWithEstate(customer)).thenReturn(List.of(account));
        when(estateRedisService.getMultipleRedisEstatePrice(any()))
                .thenReturn(Map.of(1L, redisEstatePrice));

        GetCustomerResponse response = customerService.getCustomer(id);

        assertEquals("홍길동", response.getCustomerName());
    }

    @Test
    @DisplayName("청약 내역 조회")
    void getCustomerSubscription_shouldReturnSubscriptions() {
        Long id = 1L;

        Estate estate = Estate.builder()
                .estateId(1L)
                .estateName("테스트 부동산")
                .build();

        Subscription sub = Subscription.builder()
                .subId(1L)
                .estate(estate)
                .build();

        // RedisEstatePrice도 같이 넣어줘야 함
        RedisEstatePrice redisEstatePrice = RedisEstatePrice.builder()
                .estateTokenPrice(100L)
                .estatePrice(1000000L)
                .build();

        when(subscriptionRepository.findByCustomerIdWithEstate(id)).thenReturn(List.of(sub));
        when(estateRedisService.getMultipleRedisEstatePrice(any()))
                .thenReturn(Map.of(1L, redisEstatePrice)); // estateId가 1L인 RedisEstatePrice 반환

        List<GetCustomerSubscriptionResponse> result = customerService.getCustomerSubscription(id);

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("거래 내역 조회")
    void getCustomerTrade_shouldReturnTrades() {
        Long id = 1L;

        Estate estate = Estate.builder()
                .estateId(1L)
                .estateName("빌딩A")
                .build();

        Trade trade = Trade.builder()
                .tradeId(1L)
                .estate(estate)
                .tradeTokenAmount(10L)
                .tradeDate(LocalDateTime.now())
                .build();

        RedisEstatePrice redisEstatePrice = RedisEstatePrice.builder()
                .estateTokenPrice(100L)
                .estatePrice(1000000L)
                .build();

        when(tradeRepository.findBySellerIdWithEstate(id)).thenReturn(List.of(trade));
        when(tradeRepository.findByBuyerIdWithEstate(id)).thenReturn(List.of(trade));
        when(estateRedisService.getMultipleRedisEstatePrice(any()))
                .thenReturn(Map.of(1L, redisEstatePrice));

        List<GetCustomerTradeResponse> result = customerService.getCustomerTrade(id);

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("계좌 정보 조회")
    void getCustomerAccount_shouldReturnAccountResponse() {
        Long customerId = 1L;

        // customer 및 estate 포함
        Customer customer = Customer.builder()
                .customerId(customerId)
                .customerName("홍길동")
                .build();

        Estate estate = Estate.builder()
                .estateId(1L)
                .estateName("테스트 부동산")
                .build();

        Account account = Account.builder()
                .accountId(1L)
                .accountTokenAmount(10L)
                .customer(customer)
                .estate(estate)
                .build();

        RedisEstatePrice redisEstatePrice = RedisEstatePrice.builder()
                .estateTokenPrice(100L)
                .estatePrice(1000000L)
                .build();

        when(accountRepository.findByCustomerIdWithEstate(customerId)).thenReturn(List.of(account));
        when(estateRedisService.getMultipleRedisEstatePrice(any()))
                .thenReturn(Map.of(1L, redisEstatePrice));

        List<GetCustomerAccountResponse> response = customerService.getCustomerAccount(customerId);

        assertThat(response).isNotEmpty();
    }
}
