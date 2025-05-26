package com.piehouse.woorepie.estate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class GetEstateDetailsResponse {

    private Long estateId;

    private Long agentId;

    private String agentName;

    private String estateName;

    private String businessName;

    private String estateState;

    private String estateCity;

    private String estateAddress;

    private String estateDescription;

    private String estateLatitude;

    private String estateLongitude;

    private String estateImageUrl;

    private Integer estatePrice;

    private Integer tokenAmount;

    private Integer estateTokenPrice;

    private BigDecimal dividendYield;

    private String estateUseZone;

    private BigDecimal totalEstateArea;

    private BigDecimal tradedEstateArea;

    private String subGuideUrl;

    private String securitiesReportUrl;

    private String investmentExplanationUrl;

    private String propertyMngContractUrl;

    private String appraisalReportUrl;

}
