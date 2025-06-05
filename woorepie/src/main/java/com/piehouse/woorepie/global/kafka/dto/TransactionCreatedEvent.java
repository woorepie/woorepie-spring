package com.piehouse.woorepie.global.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TransactionCreatedEvent {
    private Long estateId;
    private Long tradeId;
    private Long sellerId;
    private Long buyerId;
    private Long tokenPrice;
    private Long tradeTokenAmount;
    private LocalDateTime tradeDate;
}