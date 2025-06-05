package com.piehouse.woorepie.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GetCustomerTradeResponse {

    private Long tradeId;

    private Long estateId;

    private String estateName;

    private Long tradeTokenAmount;

    private Long tradeTokenPrice;

    private LocalDateTime tradeDate;

    private String tradeType;

}
