package com.piehouse.woorepie.global.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class S3AgentRequest {

    @NotBlank
    @Email
    private String agentEmail;

    @NotBlank
    private String identificationFileType;

    @NotBlank
    private String certFileType;

    @NotBlank
    private String warrantFileType;

}
