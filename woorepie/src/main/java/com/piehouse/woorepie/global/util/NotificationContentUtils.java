package com.piehouse.woorepie.global.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationContentUtils {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초");

    // 매도 알림 (체결)
    public static NotificationMessage createSellNotification(
            String customerName,
            String estateName,
            int price,
            int tokenAmount,
            LocalDateTime tradeTime
    ) {
        String title = "[Woorepie] 매도 체결 안내";
        String content = String.format(
                "%s 고객님, 아래 매물에 대한 매도 주문이 체결되었습니다.\n\n- 매물명: %s\n- 체결 금액: %,d원\n- 체결 수량: %d 토큰\n- 체결 일시: %s\n\n감사합니다.",
                customerName, estateName, price, tokenAmount, tradeTime.format(formatter)
        );
        return new NotificationMessage(title, content);
    }

    // 매수 알림 (체결)
    public static NotificationMessage createBuyNotification(
            String customerName,
            String estateName,
            int price,
            int tokenAmount,
            LocalDateTime tradeTime
    ) {
        String title = "[Woorepie] 매수 체결 안내";
        String content = String.format(
                "%s 고객님, 아래 매물에 대한 매수 주문이 체결되었습니다.\n\n- 매물명: %s\n- 체결 금액: %,d원\n- 체결 수량: %d 토큰\n- 체결 일시: %s\n\n감사합니다.",
                customerName, estateName, price, tokenAmount, tradeTime.format(formatter)
        );
        return new NotificationMessage(title, content);
    }

    // 청약 성공 알림
    public static NotificationMessage createSubscriptionSuccessNotification(
            String customerName,
            String estateName,
            int price,
            int tokenAmount,
            LocalDateTime tradeTime
    ) {
        String title = "[Woorepie] 청약 체결 안내";
        String content = String.format(
                "%s 고객님, 아래 매물에 대한 청약이 체결되었습니다.\n\n- 매물명: %s\n- 체결 금액: %,d원\n- 체결 수량: %d 토큰\n- 체결 일시: %s\n\n감사합니다.",
                customerName, estateName, price, tokenAmount, tradeTime.format(formatter)
        );
        return new NotificationMessage(title, content);
    }

    // 청약 실패 (모집 미달)
    public static NotificationMessage createSubscriptionFailLackNotification(
            String customerName,
            String estateName,
            int price,
            int tokenAmount,
            LocalDateTime tradeTime
    ) {
        String title = "[Woorepie] 청약 미체결 안내";
        String content = String.format(
                "%s 고객님, 아래 매물에 대한 청약이 모집 미달로 미체결되었습니다. 신청 금액은 환불 처리되었습니다.\n\n- 매물명: %s\n- 신청 금액: %,d원\n- 신청 수량: %d 토큰\n- 신청 일시: %s\n\n감사합니다.",
                customerName, estateName, price, tokenAmount, tradeTime.format(formatter)
        );
        return new NotificationMessage(title, content);
    }

    // 청약 실패 (선착순 마감)
    public static NotificationMessage createSubscriptionFailSoldoutNotification(
            String customerName,
            String estateName,
            int price,
            int tokenAmount,
            LocalDateTime tradeTime
    ) {
        String title = "[Woorepie] 청약 미체결 안내";
        String content = String.format(
                "%s 고객님, 아래 매물에 대한 청약이 선착순 마감으로 미체결되었습니다. 신청 금액은 환불 처리되었습니다.\n\n- 매물명: %s\n- 신청 금액: %,d원\n- 신청 수량: %d 토큰\n- 신청 일시: %s\n\n감사합니다.",
                customerName, estateName, price, tokenAmount, tradeTime.format(formatter)
        );
        return new NotificationMessage(title, content);
    }

    // 알림 제목/내용 한 번에 리턴하는 내부 클래스
    public static class NotificationMessage {
        public final String title;
        public final String content;
        public NotificationMessage(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}