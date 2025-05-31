package com.piehouse.woorepie.global.kafka.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DividendAcceptEvent {

    private Long estateId;

    private Integer dividend;

}