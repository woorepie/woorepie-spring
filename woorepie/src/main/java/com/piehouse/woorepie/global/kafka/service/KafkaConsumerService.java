package com.piehouse.woorepie.global.kafka.service;

import com.piehouse.woorepie.global.kafka.dto.*;

public interface KafkaConsumerService {

    void consumeOrderCreated(OrderCreatedEvent event);

    void consumeSubscriptionRequest(SubscriptionRequestEvent event);

    void consumeSubscriptionSuccess(CompleteEvent event);

    void consumeSubscriptionFailure(CompleteEvent event);

    void handleDividendApproval(DividendAcceptEvent message);

    void handleExitApproval(CompleteEvent message);

}
