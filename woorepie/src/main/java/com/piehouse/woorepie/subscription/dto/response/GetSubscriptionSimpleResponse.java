package com.piehouse.woorepie.subscription.dto.response;

import com.piehouse.woorepie.estate.entity.EstateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GetSubscriptionSimpleResponse {

    private Long estateId;

    private String estateName;

    private Long agentId;

    private String agentName;

    private String businessName;

    private LocalDateTime subStartDate;

    private LocalDateTime subEndDate;

    private String estateState;

    private String estateCity;

    private String estateImageUrl;

    private Long tokenAmount;

    private Long estatePrice;

    private Long estateTokenPrice;

    private BigDecimal dividendYield;

    private EstateStatus subState;

}
