package com.piehouse.woorepie.trade.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisCustomerTradeValue {

    private Long estateId;

    private long tradeTokenAmount ;

    private long tokenPrice;

    private long timestamp;

}
