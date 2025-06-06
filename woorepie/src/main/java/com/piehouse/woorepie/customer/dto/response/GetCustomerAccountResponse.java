package com.piehouse.woorepie.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class GetCustomerAccountResponse {

    private Long accountId;

    private Long estateId;

    private String estateName;

    private Long accountTokenAmount;

    private Long accountTokenPrice;

    private Long estateTokenPrice;

    private Long estatePrice;

}
