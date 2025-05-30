package com.piehouse.woorepie.subscription.service;

import com.piehouse.woorepie.subscription.dto.request.RegisterEstateRequest;
import com.piehouse.woorepie.subscription.dto.response.GetSubscriptionDetailsResponse;
import com.piehouse.woorepie.subscription.dto.response.GetSubscriptionSimpleResponse;

import java.util.List;

public interface SubscriptionService {
    //청약 매물 등록
    void registerEstate(RegisterEstateRequest request, Long agentId);

    //청약 매물 리스트 조회
    List<GetSubscriptionSimpleResponse> getActiveSubscriptions();
    
    //청약 매물 상세정보 조회
    GetSubscriptionDetailsResponse getSubscriptionDetails(Long estateId);

    // 청약 모집 완료에 따른 처리
    void updateSubscriptionsOnSuccess(Long estateId);

    // 청약 모집 실패에 따른 처리
    void updateSubscriptionsOnFailure(Long estateId);
}
