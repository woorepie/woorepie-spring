package com.piehouse.woorepie.notification.service.implement;

import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.notification.dto.response.NotificationResponse;
import com.piehouse.woorepie.notification.entity.Notification;
import com.piehouse.woorepie.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("NotificationServiceImpl 단위테스트")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * [정상 케이스] 읽지 않은 알림 조회
     * - DB에서 읽지 않은 알림만 정상적으로 조회되는지 검증
     */
    @Test
    @DisplayName("읽지 않은 알림 조회")
    void getUnreadNotifications_success() {
        Notification notification = mock(Notification.class);
        when(notificationRepository.findByCustomer_CustomerIdAndIsReadFalse(1L)).thenReturn(List.of(notification));
        when(notification.getNotificationId()).thenReturn(10L);
        when(notification.getTitle()).thenReturn("타이틀");
        when(notification.getContent()).thenReturn("내용");
        when(notification.getCreatedAt()).thenReturn(LocalDateTime.now());

        // when
        List<NotificationResponse> result = notificationService.getUnreadNotifications(1L);

        // then
        assertThat(result).hasSize(1);
        verify(notificationRepository).findByCustomer_CustomerIdAndIsReadFalse(1L);
    }

    /**
     * [정상 케이스] 전체 알림(읽음/안읽음) 조회
     * - DB에서 모든 알림이 내림차순으로 반환되는지 검증
     */
    @Test
    @DisplayName("전체 알림 조회")
    void getAllNotifications_success() {
        Notification notification = mock(Notification.class);
        when(notificationRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));
        when(notification.getNotificationId()).thenReturn(10L);
        when(notification.getTitle()).thenReturn("타이틀");
        when(notification.getContent()).thenReturn("내용");
        when(notification.getCreatedAt()).thenReturn(LocalDateTime.now());

        // when
        List<NotificationResponse> result = notificationService.getAllNotifications(1L);

        // then
        assertThat(result).hasSize(1);
        verify(notificationRepository).findByCustomer_CustomerIdOrderByCreatedAtDesc(1L);
    }

    /**
     * [정상 케이스] 알림 읽음 처리
     * - 특정 알림을 읽음 처리할 때 isRead가 true로 바뀌는지 검증
     */
    @Test
    @DisplayName("알림 읽음 처리")
    void markAsRead_success() {
        Notification notification = mock(Notification.class);
        when(notificationRepository.findById(99L)).thenReturn(Optional.of(notification));
        doNothing().when(notification).markAsRead();

        // when
        notificationService.markAsRead(99L);

        // then
        verify(notificationRepository).findById(99L);
        verify(notification).markAsRead();
    }

    /**
     * [예외 케이스] 알림 읽음 처리 - 알림 미존재
     */
    @Test
    @DisplayName("알림 읽음 처리 - 미존재 예외")
    void markAsRead_notExist_fail() {
        when(notificationRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(77L))
                .isInstanceOf(CustomException.class);
    }

    /**
     * [정상 케이스] 매수/매도 거래 알림 발송
     * - 거래 체결 시 알림이 DB에 정상 저장되는지 검증 (isBuy true/false)
     */
    @Test
    @DisplayName("거래 체결 알림 - 매수/매도")
    void sendTradeNotification_success() {
        Customer customer = mock(Customer.class);
        when(customer.getCustomerId()).thenReturn(123L);
        when(customer.getCustomerName()).thenReturn("홍길동");

        Notification notification = mock(Notification.class);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notification.getNotificationId()).thenReturn(1L);

        // 매수 알림
        assertThatNoException().isThrownBy(() ->
                notificationService.sendTradeNotification(customer, "부동산1", 1000, 2, LocalDateTime.now(), true)
        );

        // 매도 알림
        assertThatNoException().isThrownBy(() ->
                notificationService.sendTradeNotification(customer, "부동산1", 2000, 1, LocalDateTime.now(), false)
        );

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    /**
     * [정상 케이스] 청약 성공 알림
     */
    @Test
    @DisplayName("청약 성공 알림")
    void sendSubscriptionSuccessNotification_success() {
        Customer customer = mock(Customer.class);
        when(customer.getCustomerName()).thenReturn("홍길동");

        Notification notification = mock(Notification.class);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        assertThatNoException().isThrownBy(() ->
                notificationService.sendSubscriptionSuccessNotification(customer, "부동산1", 1000, 2, LocalDateTime.now())
        );
        verify(notificationRepository).save(any(Notification.class));
    }

    /**
     * [정상 케이스] 청약 실패(모집 미달) 알림
     */
    @Test
    @DisplayName("청약 실패(미달) 알림")
    void sendSubscriptionFailLackNotification_success() {
        Customer customer = mock(Customer.class);
        when(customer.getCustomerName()).thenReturn("홍길동");

        Notification notification = mock(Notification.class);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        assertThatNoException().isThrownBy(() ->
                notificationService.sendSubscriptionFailLackNotification(customer, "부동산1", 1000, 2, LocalDateTime.now())
        );
        verify(notificationRepository).save(any(Notification.class));
    }

    /**
     * [정상 케이스] 청약 실패(선착순 마감) 알림
     */
    @Test
    @DisplayName("청약 실패(선착순 마감) 알림")
    void sendSubscriptionFailSoldoutNotification_success() {
        Customer customer = mock(Customer.class);
        when(customer.getCustomerName()).thenReturn("홍길동");

        Notification notification = mock(Notification.class);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        assertThatNoException().isThrownBy(() ->
                notificationService.sendSubscriptionFailSoldoutNotification(customer, "부동산1", 1000, 2, LocalDateTime.now())
        );
        verify(notificationRepository).save(any(Notification.class));
    }

    /**
     * [정상 케이스] 매각 환불 알림
     */
    @Test
    @DisplayName("매각 환불 알림")
    void sendSellRefundNotification_success() {
        Customer customer = mock(Customer.class);
        when(customer.getCustomerName()).thenReturn("홍길동");

        Notification notification = mock(Notification.class);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        assertThatNoException().isThrownBy(() ->
                notificationService.sendSellRefundNotification(customer, "부동산1", 3000, 3, LocalDateTime.now())
        );
        verify(notificationRepository).save(any(Notification.class));
    }

}
