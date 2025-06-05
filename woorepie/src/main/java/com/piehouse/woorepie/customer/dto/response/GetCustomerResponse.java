package com.piehouse.woorepie.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GetCustomerResponse {

    private String customerName;

    private String customerEmail;

    private String customerPhoneNumber;

    private String customerAddress;

    private String accountNumber;

    private Long accountBalance;

    private Long totalAccountTokenPrice;

    private LocalDateTime customerJoinDate;

}
