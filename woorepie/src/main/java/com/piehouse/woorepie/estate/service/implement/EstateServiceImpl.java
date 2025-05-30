package com.piehouse.woorepie.estate.service.implement;

import com.piehouse.woorepie.estate.dto.request.ModifyEstateRequest;
import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.dto.response.GetEstateDetailsResponse;
import com.piehouse.woorepie.estate.dto.response.GetEstatePriceResponse;
import com.piehouse.woorepie.estate.dto.response.GetEstateSimpleResponse;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstatePrice;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import com.piehouse.woorepie.estate.repository.EstatePriceRepository;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.estate.service.EstateService;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstateServiceImpl implements EstateService {

    private final EstateRepository estateRepository;
    private final EstatePriceRepository estatePriceRepository;
    private final EstateRedisServiceImpl estateRedisServiceImpl;

    // 매물 리스트 조회
    @Override
    @Transactional(readOnly = true)
    public List<GetEstateSimpleResponse> getTradableEstates() {

        List<Estate> estates = estateRepository.findByEstateStatus(EstateStatus.SUCCESS);

        List<Long> estateIds = estates.stream()
                .map(Estate::getEstateId)
                .toList();

        Map<Long, RedisEstatePrice> estatePriceMap = estateRedisServiceImpl.getMultipleRedisEstatePrice(estateIds);

        return estates.stream()
                .map(estate -> {

                    RedisEstatePrice price = estatePriceMap.get(estate.getEstateId());

                    return GetEstateSimpleResponse.builder()
                            .estateId(estate.getEstateId())
                            .estateName(estate.getEstateName())
                            .estateState(estate.getEstateState())
                            .estateCity(estate.getEstateCity())
                            .dividendYield(price.getDividendYield())
                            .tokenAmount(price.getTokenAmount())
                            .estateTokenPrice(price.getEstateTokenPrice())
                            .estateRegistrationDate(estate.getEstateRegistrationDate())
                            .estateImageUrl(estate.getEstateImageUrl())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 매물 상세내역 조회
    @Override
    @Transactional(readOnly = true)
    public GetEstateDetailsResponse getTradableEstateDetails(Long estateId) {
        Estate estate = estateRepository.findById(estateId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESTATE_NOT_FOUND));

        RedisEstatePrice price = estateRedisServiceImpl.getRedisEstatePrice(estateId);

        return GetEstateDetailsResponse.builder()
                .estateId(estate.getEstateId())
                .agentId(estate.getAgent().getAgentId())
                .agentName(estate.getAgent().getAgentName())
                .estateName(estate.getEstateName())
                .businessName(estate.getAgent().getBusinessName())
                .estateState(estate.getEstateState())
                .estateCity(estate.getEstateCity())
                .estateAddress(estate.getEstateAddress())
                .estateDescription(estate.getEstateDescription())
                .estateLatitude(estate.getEstateLatitude())
                .estateLongitude(estate.getEstateLongitude())
                .estateImageUrl(estate.getEstateImageUrl())
                .estatePrice(price.getEstatePrice())
                .tokenAmount(estate.getTokenAmount())
                .dividendYield(price.getDividendYield())
                .estateTokenPrice(price.getEstateTokenPrice())
                .estateDescription(estate.getEstateDescription())
                .estateUseZone(estate.getEstateUseZone())
                .totalEstateArea(estate.getTotalEstateArea())
                .tradedEstateArea(estate.getTradedEstateArea())
                .subGuideUrl(estate.getSubGuideUrl())
                .securitiesReportUrl(estate.getSecuritiesReportUrl())
                .investmentExplanationUrl(estate.getInvestmentExplanationUrl())
                .propertyMngContractUrl(estate.getPropertyMngContractUrl())
                .appraisalReportUrl(estate.getAppraisalReportUrl())
                .build();
    }

    // 매물 시세 내역 조회
    @Override
    @Transactional(readOnly = true)
    public List<GetEstatePriceResponse> getEstatePriceHistory(Long estateId) {

        // 시세 내역 조회
        List<EstatePrice> priceList = estatePriceRepository
                .findAllByEstate_EstateIdOrderByEstatePriceDateDesc(estateId);

        if (priceList.isEmpty()) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND); // 시세 없음
        }

        // 응답 DTO 변환
        return priceList.stream()
                .map(p -> GetEstatePriceResponse.builder()
                        .estatePrice(p.getEstatePrice())
                        .estatePriceDate(p.getEstatePriceDate())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void modifyEstateDescription(Long agentId, ModifyEstateRequest request) {

        Estate estate = estateRepository.findById(request.getEstateId())
                .orElseThrow(() -> new CustomException(ErrorCode.ESTATE_NOT_FOUND));

        estate.updateDescription(request.getEstateDescription());

    }


}
