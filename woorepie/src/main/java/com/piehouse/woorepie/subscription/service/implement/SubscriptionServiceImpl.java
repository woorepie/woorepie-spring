package com.piehouse.woorepie.subscription.service.implement;

import com.piehouse.woorepie.agent.entity.Agent;
import com.piehouse.woorepie.agent.repository.AgentRepository;
import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstatePrice;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import com.piehouse.woorepie.estate.repository.EstatePriceRepository;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.estate.service.implement.EstateRedisServiceImpl;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.global.service.implement.S3ServiceImpl;
import com.piehouse.woorepie.subscription.dto.request.RegisterEstateRequest;
import com.piehouse.woorepie.subscription.dto.response.GetSubscriptionDetailsResponse;
import com.piehouse.woorepie.subscription.dto.response.GetSubscriptionSimpleResponse;
import com.piehouse.woorepie.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final EstateRepository estateRepository;
    private final EstatePriceRepository estatePriceRepository;
    private final AgentRepository agentRepository;
    private final EstateRedisServiceImpl  estateRedisServiceImpl;
    private final S3ServiceImpl s3serviceImpl;

    @Override
    @Transactional
    public void registerEstate(RegisterEstateRequest request, Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Estate estate = Estate.builder()
                .agent(agent)
                .estateName(request.getEstateName())
                .estateState(request.getEstateState())
                .estateCity(request.getEstateCity())
                .estateAddress(request.getEstateAddress())
                .estateLatitude(request.getEstateLatitude())
                .estateLongitude(request.getEstateLongitude())
                .totalEstateArea(request.getTotalEstateArea())
                .tradedEstateArea(request.getTradeEstateArea())
                .estateUseZone(request.getEstateUseZone())
                .estateDescription(request.getEstateDescription())
                .estateImageUrl(s3serviceImpl.getPublicS3Url(request.getEstateImageUrlKey()))
                .subGuideUrl(s3serviceImpl.getPublicS3Url(request.getSubGuideUrlKey()))
                .securitiesReportUrl(s3serviceImpl.getPublicS3Url(request.getSecuritiesReportUrlKey()))
                .investmentExplanationUrl(s3serviceImpl.getPublicS3Url(request.getInvestmentExplanationUrlKey()))
                .propertyMngContractUrl(s3serviceImpl.getPublicS3Url(request.getPropertyMngContractUrlKey()))
                .appraisalReportUrl(s3serviceImpl.getPublicS3Url(request.getAppraisalReportUrlKey()))
                .estateRegistrationDate(LocalDateTime.now())
                .tokenAmount(request.getTokenAmount())
                .estateStatus(EstateStatus.READY)
                .build();
        estateRepository.save(estate);

    //매물 시세 테이블
        EstatePrice estatePrice = EstatePrice.builder()
                .estate(estate)
                .estatePrice(request.getEstatePrice())
                .estatePriceDate(LocalDateTime.now())
                .build();
        estatePriceRepository.save(estatePrice);

    }
    
    //청약 가능한 매물 리스트 조회
    @Override
    @Transactional(readOnly = true)
    public List<GetSubscriptionSimpleResponse> getActiveSubscriptions() {

        List<Estate> estates = estateRepository.findBySubStateIn(List.of(
                EstateStatus.READY, EstateStatus.RUNNING, EstateStatus.PENDING, EstateStatus.FAILURE
        )); // 청약 중인 substate 필터링

        List<Long> estateIds = estates.stream()
                .map(Estate::getEstateId)
                .toList();

        Map<Long, RedisEstatePrice> estatePriceMap = estateRedisServiceImpl.getMultipleRedisEstatePrice(estateIds);


        return estates.stream()
                .map(estate -> {
                    RedisEstatePrice price = estatePriceMap.get(estate.getEstateId());

                    return GetSubscriptionSimpleResponse.builder()
                            .estateId(estate.getEstateId())
                            .estateName(estate.getEstateName())
                            .agentId(estate.getAgent().getAgentId())
                            .agentName(estate.getAgent().getAgentName())
                            .subStartDate(estate.getSubStartDate())
                            .subEndDate(estate.getSubEndDate())
                            .estateState(estate.getEstateState())
                            .estateCity(estate.getEstateCity())
                            .estateImageUrl(estate.getEstateImageUrl())
                            .tokenAmount(price.getTokenAmount())
                            .estatePrice(price.getEstatePrice())
                            .estateTokenPrice(price.getEstateTokenPrice())
                            .dividendYield(price.getDividendYield())
                            .estateStatus(estate.getEstateStatus())
                            .build();
                })
                .collect(Collectors.toList());

    }

    // 청약 매물 상세정보 조회
    @Override
    @Transactional(readOnly = true)
    public GetSubscriptionDetailsResponse getSubscriptionDetails(Long estateId) {
        Estate estate = estateRepository.findById(estateId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESTATE_NOT_FOUND));

        RedisEstatePrice price = estateRedisServiceImpl.getRedisEstatePrice(estateId);

        int subTokenAmount = estate.getTokenAmount();

        return GetSubscriptionDetailsResponse.builder()
                .estateId(estate.getEstateId())
                .estateName(estate.getEstateName())
                .agentId(estate.getAgent().getAgentId())
                .agentName(estate.getAgent().getAgentName())
                .businessName(estate.getAgent().getBusinessName())
                .subStartDate(estate.getSubStartDate())
                .subEndDate(estate.getSubEndDate())
                .estateState(estate.getEstateState())
                .estateCity(estate.getEstateCity())
                .estateAddress(estate.getEstateAddress())
                .estateDescription(estate.getEstateDescription())
                .estateLatitude(estate.getEstateLatitude())
                .estateLongitude(estate.getEstateLongitude())
                .estateImageUrl(estate.getEstateImageUrl())
                .estatePrice(price.getEstatePrice())
                .tokenAmount(estate.getTokenAmount())
                .subTokenAmount(subTokenAmount)
                .estateTokenPrice(price.getEstateTokenPrice())
                .dividendYield(price.getDividendYield())
                .estateStatus(estate.getEstateStatus())
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

}
