package com.piehouse.woorepie.customer.dto.response;

import com.piehouse.woorepie.subscription.entity.SubStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GetCustomerSubscriptionResponse {

    private Long subId;

    private Long estateId;

    private String estateName;

    private Long subTokenAmount;

    private Long subTokenPrice;

    private LocalDateTime subDate;

    private SubStatus subStatus;

}
