package com.piehouse.woorepie.global.kafka.service.impliment;

import com.piehouse.woorepie.global.kafka.dto.*;
import com.piehouse.woorepie.global.kafka.service.KafkaProducerService;
import com.piehouse.woorepie.global.util.KafkaRetryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaRetryUtil kafkaRetryUtil;
    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String TRANSACTION_CREATED_TOPIC = "transaction.created";
    private static final String CUSTOMER_CREATED_TOPIC = "customer.created";
    private static final String SUBSCRIPTION_REQUEST_TOPIC = "subscription.request";
    private static final String SUBSCRIPTION_ACCEPT_TOPIC = "subscription.accept";

    // Kafka에 매수, 매도 요청 이벤트 보내기
    @Override
    public void sendOrderCreated(OrderCreatedEvent event) {
        sendWithKey(ORDER_CREATED_TOPIC, event.getEstateId().toString(), event);
    }

    // kafka에 customer 회원가입 이벤트 보내기
    @Override
    public void sendCustomerCreated(CustomerCreatedEvent event) {
        send(CUSTOMER_CREATED_TOPIC, event);
    }

    // Kafka에 거래 체결 이벤트 보내기
    @Override
    public void sendTransactionCreated(TransactionCreatedEvent event) {
        send(TRANSACTION_CREATED_TOPIC, event);
    }

    // Kafka에 청약 요청 이벤트 보내기
    @Override
    public void sendSubscriptionRequest(SubscriptionRequestEvent event) {
        sendWithKey(SUBSCRIPTION_REQUEST_TOPIC, event.getEstateId().toString(), event); // 공통 send() 사용
    }

    // kafka에 청약 성공 결과 이벤트 보내기
    @Override
    public void sendSubscriptionAccept(SubscriptionAcceptEvent event) {
        send(SUBSCRIPTION_ACCEPT_TOPIC, event);
    }

    private <T> void send(String topic, T event) {
        kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Kafka 전송 성공 [{}]: {}", topic, event.toString());
                    } else {
                        log.warn("Kafka 전송 실패 [{}], retrying...", topic, ex);
                        kafkaRetryUtil.sendWithRetry(topic, event, 3);
                    }
                });
    }

    private <T> void sendWithKey(String topic, String key, T event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Kafka 전송 성공 [{}][key:{}]: {}", topic, key, event);
                    } else {
                        log.warn("Kafka 전송 실패 [{}][key:{}], retrying...", topic, key, ex);
                        kafkaRetryUtil.sendWithRetry(topic, key, event, 3);
                    }
                });
    }

}
