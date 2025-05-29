package com.piehouse.woorepie.global.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class S3EstateRequest {

    @NotBlank
    private String estateAddress;

    @NotBlank
    private String imageFileType;

    @NotBlank
    private String subGuideFileType;

    @NotBlank
    private String securitiesReportFileType;

    @NotBlank
    private String investmentExplanationFileType;

    @NotBlank
    private String propertyMngContractFileType;

    @NotBlank
    private String appraisalReportFileType;

}