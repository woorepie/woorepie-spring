package com.piehouse.woorepie.estate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GetEstatePriceResponse {

    private Long estatePrice;

    private LocalDateTime estatePriceDate;

}
