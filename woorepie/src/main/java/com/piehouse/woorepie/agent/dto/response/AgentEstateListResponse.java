package com.piehouse.woorepie.agent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class AgentEstateListResponse {
    private Long estateId;
    private String estateName;
    private Integer tokenAmount;
    private Integer estateTokenPrice;
    private BigDecimal dividendYield;
    private String estateStatus;
}