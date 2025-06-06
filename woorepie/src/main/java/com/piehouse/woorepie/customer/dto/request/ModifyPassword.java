package com.piehouse.woorepie.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ModifyPassword {

    @NotBlank(message = "currentPassword 필수입니다.")
    private String currentPassword;

    @NotBlank(message = "currentPassword 필수입니다.")
    private String newPassword;

}
