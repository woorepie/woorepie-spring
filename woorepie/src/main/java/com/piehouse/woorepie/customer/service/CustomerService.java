package com.piehouse.woorepie.customer.service;

import com.piehouse.woorepie.customer.dto.request.CreateCustomerRequest;
import com.piehouse.woorepie.customer.dto.response.GetCustomerSubscriptionResponse;
import com.piehouse.woorepie.customer.dto.request.LoginCustomerRequest;
import com.piehouse.woorepie.customer.dto.response.GetCustomerAccountResponse;
import com.piehouse.woorepie.customer.dto.response.GetCustomerResponse;
import com.piehouse.woorepie.customer.dto.response.GetCustomerTradeResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface CustomerService {

    void customerLogin(LoginCustomerRequest requestDto, HttpServletRequest request);

    void customerLogout(HttpServletRequest request);

    Boolean checkCustomerEmail(String customerEmail);

    void createCustomer(CreateCustomerRequest requestDto);

    GetCustomerResponse getCustomer(Long customerId);

    List<GetCustomerAccountResponse> getCustomerAccount(Long customerId);

    List<GetCustomerSubscriptionResponse> getCustomerSubscription(Long customerId);

    List<GetCustomerTradeResponse> getCustomerTrade(Long customerId);

}
