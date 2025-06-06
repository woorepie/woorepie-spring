package com.piehouse.woorepie.trade.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionTradeRequest {

    @NotNull(message = "매물 ID는 필수입니다.")
    private Long estateId;

    @NotNull(message = "청약 수량은 필수입니다.")
    @Min(value = 1, message = "최소 1개 이상 신청해야 합니다.")
    private Long subAmount;
}
