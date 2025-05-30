package com.piehouse.woorepie.global.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionAcceptEvent {
    private Long estateId;
    private Integer tokenPrice;
    private List<CustomerInfo> customer;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerInfo {
        private Long customerId;
        private Integer tradeTokenAmount;
    }
}
