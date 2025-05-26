package com.piehouse.woorepie.subscription.dto.response;

import com.piehouse.woorepie.estate.entity.SubState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GetSubscriptionDetailsResponse {

    private Long estateId;

    private Long agentId;

    private String estateName;

    private String agentName;

    private String businessName;

    private LocalDateTime subStartDate;

    private LocalDateTime subEndDate;

    private String estateState;

    private String estateCity;

    private String estateAddress;

    private String estateDescription;

    private String estateLatitude;

    private String estateLongitude;

    private String estateImageUrl;

    private Integer estatePrice;

    private Integer tokenAmount;

    private Integer subTokenAmount;

    private Integer estateTokenPrice;

    private SubState subState;

    private String estateUseZone;

    private BigDecimal totalEstateArea;

    private BigDecimal tradedEstateArea;

    private BigDecimal dividendYield;

    private String subGuideUrl;

    private String securitiesReportUrl;

    private String investmentExplanationUrl;

    private String propertyMngContractUrl;

    private String appraisalReportUrl;

}
