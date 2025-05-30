package com.piehouse.woorepie.global.kafka.service;

import com.piehouse.woorepie.global.kafka.dto.*;

public interface KafkaProducerService {

    void sendTransactionCreated(TransactionCreatedEvent event); // 거래 체결 완료 이벤트

    void sendOrderCreated(OrderCreatedEvent event); // 매수, 매도 요청 이벤트

    void sendCustomerCreated(CustomerCreatedEvent event); // 회원가입 완료 이벤트

    void sendSubscriptionRequest(SubscriptionRequestEvent event); // 청약 신청 이벤트

    void sendSubscriptionAccept(SubscriptionAcceptEvent event); // 청약 성공 결과 이벤트
}

