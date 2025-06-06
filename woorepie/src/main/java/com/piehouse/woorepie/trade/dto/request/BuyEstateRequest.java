package com.piehouse.woorepie.trade.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyEstateRequest {

    private Long estateId;             // 어떤 매물인지

    private Long tradeTokenAmount;  // 매수할 토큰 수량

    private Long tokenPrice;        // 토큰당 가격

}
