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
    private Long tokenAmount;
    private Long estateTokenPrice;
    private BigDecimal dividendYield;
    private String estateStatus;
}