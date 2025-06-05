package com.piehouse.woorepie.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterEstateRequest {

    @NotBlank(message = "estateName 필수입니다.")
    private String estateName;

    @NotBlank(message = "estateState 필수입니다.")
    private String estateState;

    @NotBlank(message = "estateCity 필수입니다.")
    private String estateCity;

    @NotBlank(message = "estatePrice 필수입니다.")
    private Long estatePrice;

    @NotBlank(message = "estateAddress 필수입니다.")
    private String estateAddress;

    @NotBlank(message = "estateLatitude 필수입니다.")
    private String estateLatitude;

    @NotBlank(message = "estateLongitude 필수입니다.")
    private String estateLongitude;

    @NotBlank(message = "tokenAmount 필수입니다.")
    private Long tokenAmount;

    @NotBlank(message = "totalEstateArea 필수입니다.")
    private BigDecimal totalEstateArea;

    @NotBlank(message = "tradeEstateArea 필수입니다.")
    private BigDecimal tradeEstateArea;

    @NotBlank(message = "estateUseZone 필수입니다.")
    private String estateUseZone;

    private String estateDescription;

    @NotBlank(message = "estateImageUrlKey 필수입니다.")
    private String estateImageUrlKey;

    @NotBlank(message = "subGuideUrlKey 필수입니다.")
    private String subGuideUrlKey;

    @NotBlank(message = "securitiesReportUrlKey 필수입니다.")
    private String securitiesReportUrlKey;

    @NotBlank(message = "investmentExplanationUrlKey 필수입니다.")
    private String investmentExplanationUrlKey;

    @NotBlank(message = "propertyMngContractUrlKey 필수입니다.")
    private String propertyMngContractUrlKey;

    @NotBlank(message = "appraisalReportUrlKey 필수입니다.")
    private String appraisalReportUrlKey;

}
