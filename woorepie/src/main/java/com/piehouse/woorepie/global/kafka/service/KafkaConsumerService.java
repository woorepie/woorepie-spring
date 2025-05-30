package com.piehouse.woorepie.global.kafka.service;

import com.piehouse.woorepie.global.kafka.dto.*;

public interface KafkaConsumerService {

    void consumeOrderCreated(OrderCreatedEvent event);

    void consumeSubscriptionRequest(SubscriptionRequestEvent event);

    void consumeSubscriptionSuccess(SubscriptionResultEvent event);

    void consumeSubscriptionFailure(SubscriptionResultEvent event);

    void handleSubscriptionApproval(SubscriptionAcceptMessage message);

    void handleDividendApproval(DividendAcceptMessage message);

    void handleExitApproval(ExitAcceptMessage message);

}
