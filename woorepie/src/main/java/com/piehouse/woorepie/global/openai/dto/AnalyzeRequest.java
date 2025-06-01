package com.piehouse.woorepie.global.openai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnalyzeRequest {
    private String estateName;
    private String address;
    private double lat;
    private double lng;
}
