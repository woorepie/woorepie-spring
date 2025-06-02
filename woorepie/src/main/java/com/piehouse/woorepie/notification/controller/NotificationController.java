package com.piehouse.woorepie.notification.controller;

import com.piehouse.woorepie.customer.dto.SessionCustomer;
import com.piehouse.woorepie.notification.dto.response.NotificationResponse;
import com.piehouse.woorepie.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 읽지 않은 알림 목록 조회
     */
    @GetMapping("/unread")
    public List<NotificationResponse> getUnreadNotifications(@AuthenticationPrincipal SessionCustomer sessionCustomer) {
        return notificationService.getUnreadNotifications(sessionCustomer.getCustomerId());
    }

    /**
     * 전체 알림 조회 (읽은 + 안읽은)
     */
    @GetMapping
    public List<NotificationResponse> getAllNotifications(@AuthenticationPrincipal SessionCustomer sessionCustomer) {
        return notificationService.getAllNotifications(sessionCustomer.getCustomerId());
    }

    /**
     * 알림 읽음 처리
     */
    @PostMapping("/{notificationId}/read")
    public void markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
    }
}
