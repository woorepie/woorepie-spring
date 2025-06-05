package com.piehouse.woorepie.estate.service;

import com.piehouse.woorepie.estate.dto.request.ModifyEstateRequest;
import com.piehouse.woorepie.estate.dto.response.GetEstateDetailsResponse;
import com.piehouse.woorepie.estate.dto.response.GetEstatePriceResponse;
import com.piehouse.woorepie.estate.dto.response.GetEstateSimpleResponse;

import java.util.List;

public interface EstateService {

    // 청약 완료된 매물 리스트 조회 (거래 가능 매물)
    List<GetEstateSimpleResponse> getTradableEstates();
    
    // 매물 상세내역 조회
    GetEstateDetailsResponse getTradableEstateDetails(Long estateId);
    
    // 매물 시세 내역 조회
    List<GetEstatePriceResponse> getEstatePriceHistory(Long estateId);

    // 매물 수정
    void modifyEstateDescription(Long agentId, ModifyEstateRequest request);
}
