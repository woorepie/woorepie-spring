package com.piehouse.woorepie.notification.service.implement;

import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.util.NotificationContentUtils;
import com.piehouse.woorepie.notification.dto.response.NotificationResponse;
import com.piehouse.woorepie.notification.entity.Notification;
import com.piehouse.woorepie.notification.repository.NotificationRepository;
import com.piehouse.woorepie.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.piehouse.woorepie.global.exception.ErrorCode.NOTIFICATION_NON_EXIST;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    // 읽지 않은 알림 조회
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long customerId) {
        return notificationRepository.findByCustomer_CustomerIdAndIsReadFalse(customerId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    // 전체 알림 조회 (읽은/안읽은)
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications(Long customerId) {
        return notificationRepository.findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    // 알림 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(NOTIFICATION_NON_EXIST));
        notification.markAsRead();
    }

    // 거래 체결(매수/매도) 알림 전송
    @Transactional
    public void sendTradeNotification(
            Customer customer,
            String estateName,
            int price,
            int tokenAmount,
            LocalDateTime tradeTime,
            boolean isBuy
    ) {
        log.info("[알림 생성 시도] 고객 ID: {}, 매물명: {}", customer.getCustomerId(), estateName);

        // 알림 제목/내용 생성
        NotificationContentUtils.NotificationMessage message =
                isBuy
                        ? NotificationContentUtils.createBuyNotification(customer.getCustomerName(), estateName, price*tokenAmount, tokenAmount, tradeTime)
                        : NotificationContentUtils.createSellNotification(customer.getCustomerName(), estateName, price*tokenAmount, tokenAmount, tradeTime);

        log.debug("생성된 알림 - title: {}, content: {}", message.title, message.content);

        // DB 저장
        Notification notification = notificationRepository.save(
                Notification.builder()
                        .customer(customer)
                        .title(message.title)
                        .content(message.content)
                        .isRead(false)
                        .build()
        );

        log.info("[알림 저장 완료] 알림 ID: {}", notification.getNotificationId());
    }

    // 청약 성공 알림 전송
    @Transactional
    public void sendSubscriptionSuccessNotification(
            Customer customer,
            String estateName,
            int price,
            int tokenAmount,
            LocalDateTime tradeTime
    ) {
        NotificationContentUtils.NotificationMessage message =
                NotificationContentUtils.createSubscriptionSuccessNotification(
                        customer.getCustomerName(), estateName, price*tokenAmount, tokenAmount, tradeTime);

        Notification notification = notificationRepository.save(
                Notification.builder()
                        .customer(customer)
                        .title(message.title)
                        .content(message.content)
                        .isRead(false)
                        .build()
        );
    }

    // 청약 실패(모집 미달) 알림 전송
    @Transactional
    public void sendSubscriptionFailLackNotification(
            Customer customer,
            String estateName,
            int price,
            int tokenAmount,
            LocalDateTime tradeTime
    ) {
        NotificationContentUtils.NotificationMessage message =
                NotificationContentUtils.createSubscriptionFailLackNotification(
                        customer.getCustomerName(), estateName, price*tokenAmount, tokenAmount, tradeTime);

        Notification notification = notificationRepository.save(
                Notification.builder()
                        .customer(customer)
                        .title(message.title)
                        .content(message.content)
                        .isRead(false)
                        .build()
        );
    }

    // 청약 실패(선착순 마감) 알림 전송
    @Transactional
    public void sendSubscriptionFailSoldoutNotification(
            Customer customer,
            String estateName,
            int price,
            int tokenAmount,
            LocalDateTime tradeTime
    ) {
        NotificationContentUtils.NotificationMessage message =
                NotificationContentUtils.createSubscriptionFailSoldoutNotification(
                        customer.getCustomerName(), estateName, price*tokenAmount, tokenAmount, tradeTime);

        Notification notification = notificationRepository.save(
                Notification.builder()
                        .customer(customer)
                        .title(message.title)
                        .content(message.content)
                        .isRead(false)
                        .build()
        );
    }
}