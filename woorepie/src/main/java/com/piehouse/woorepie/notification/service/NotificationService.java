package com.piehouse.woorepie.notification.service;

import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.notification.dto.response.NotificationResponse;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


public interface NotificationService {
    // 읽지 않은 알림 조회
    List<NotificationResponse> getUnreadNotifications(Long customerId);

    // 전체 알림 조회 (읽은/안읽은)
    List<NotificationResponse> getAllNotifications(Long customerId);

    // 알림 읽음 처리
    void markAsRead(Long notificationId);

    // 거래 체결(매수/매도) 알림
    void sendTradeNotification(
            Customer customer,
            String estateName,
            long price,
            long tokenAmount,
            LocalDateTime tradeTime,
            boolean isBuy
    );

    // 청약 성공 알림
    void sendSubscriptionSuccessNotification(
            Customer customer,
            String estateName,
            long price,
            long tokenAmount,
            LocalDateTime tradeTime
    );

    // 청약 실패(모집 미달) 알림
    void sendSubscriptionFailLackNotification(
            Customer customer,
            String estateName,
            long price,
            long tokenAmount,
            LocalDateTime tradeTime
    );

    // 청약 실패(선착순 마감) 알림
    void sendSubscriptionFailSoldoutNotification(
            Customer customer,
            String estateName,
            long price,
            long tokenAmount,
            LocalDateTime tradeTime
    );

    // 매각 알림
    void sendSellRefundNotification(
            Customer customer,
            String estateName,
            long refundAmount,
            long tokenAmount,
            LocalDateTime refundTime
    );
}
