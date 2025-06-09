package com.piehouse.woorepie.global.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionAcceptEvent {
    private Long estateId;
    private Long customerId;
    private Long tokenPrice;
    private Long tradeTokenAmount;
}
