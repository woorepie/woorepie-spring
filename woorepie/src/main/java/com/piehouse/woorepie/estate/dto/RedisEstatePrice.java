package com.piehouse.woorepie.estate.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RedisEstatePrice {

    private Long estatePrice;

    private Long estateTokenPrice;

    private Long tokenAmount;

    private BigDecimal dividendYield;

}
