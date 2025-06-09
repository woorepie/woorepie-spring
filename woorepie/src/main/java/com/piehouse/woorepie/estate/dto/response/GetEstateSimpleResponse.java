package com.piehouse.woorepie.estate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GetEstateSimpleResponse {

    private Long estateId;

    private String estateName;

    private String estateState;

    private String estateCity;

    private String estateImageUrl;

    private BigDecimal dividendYield;

    private Long tokenAmount;

    private Long estateTokenPrice;

    private LocalDateTime estateRegistrationDate;

}
