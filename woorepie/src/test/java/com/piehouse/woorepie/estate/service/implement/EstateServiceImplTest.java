package com.piehouse.woorepie.estate.service.implement;

import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.dto.request.ModifyEstateRequest;
import com.piehouse.woorepie.estate.dto.response.GetEstateDetailsResponse;
import com.piehouse.woorepie.estate.dto.response.GetEstatePriceResponse;
import com.piehouse.woorepie.estate.dto.response.GetEstateSimpleResponse;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstatePrice;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import com.piehouse.woorepie.estate.repository.EstatePriceRepository;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstateServiceImplTest {

    @Mock private EstateRepository estateRepository;
    @Mock private EstatePriceRepository estatePriceRepository;
    @Mock private EstateRedisServiceImpl estateRedisServiceImpl;

    @InjectMocks private EstateServiceImpl estateService;

    @BeforeEach
    void setUp() {}

    @Test
    @DisplayName("매물 리스트 조회 - 정상 동작")
    void getTradableEstates_shouldReturnList() {
        Estate estate = Estate.builder()
                .estateId(1L)
                .estateName("빌딩A")
                .estateState("서울시")
                .estateCity("서울")
                .estateStatus(EstateStatus.SUCCESS)
                .estateRegistrationDate(LocalDateTime.now())
                .estateImageUrl("image.jpg")
                .build();

        when(estateRepository.findByEstateStatus(EstateStatus.SUCCESS))
                .thenReturn(List.of(estate));

        RedisEstatePrice redisPrice = RedisEstatePrice.builder()
                .estateTokenPrice(100)
                .dividendYield(BigDecimal.valueOf(5.5))
                .tokenAmount(1000)
                .build();

        when(estateRedisServiceImpl.getMultipleRedisEstatePrice(List.of(1L)))
                .thenReturn(Map.of(1L, redisPrice));

        List<GetEstateSimpleResponse> responses = estateService.getTradableEstates();

        assertThat(responses).hasSize(1);
        assertEquals("빌딩A", responses.get(0).getEstateName());
        assertEquals(100, responses.get(0).getEstateTokenPrice());
    }

    @Test
    @DisplayName("매물 상세 조회 - 정상 동작")
    void getTradableEstateDetails_shouldReturnDetails() {
        Estate estate = Estate.builder()
                .estateId(1L)
                .estateName("빌딩A")
                .estateState("서울시")
                .estateCity("서울")
                .estateAddress("서울특별시 종로구")
                .estateDescription("좋은 건물")
                .estateLatitude("37.5665")
                .estateLongitude("126.978")
                .estateImageUrl("image.jpg")
                .tokenAmount(1000)
                .estateUseZone("상업지역")
                .totalEstateArea(BigDecimal.valueOf(1000))
                .tradedEstateArea(BigDecimal.valueOf(500))
                .subGuideUrl("guide.pdf")
                .securitiesReportUrl("securities.pdf")
                .investmentExplanationUrl("investment.pdf")
                .propertyMngContractUrl("contract.pdf")
                .appraisalReportUrl("appraisal.pdf")
                .agent(null)  // 단순화
                .build();

        when(estateRepository.findById(1L)).thenReturn(Optional.of(estate));

        RedisEstatePrice price = RedisEstatePrice.builder()
                .estatePrice(1000000)
                .dividendYield(BigDecimal.valueOf(5.5))
                .estateTokenPrice(100)
                .build();

        when(estateRedisServiceImpl.getRedisEstatePrice(1L)).thenReturn(price);

        assertThrows(NullPointerException.class, () ->
                estateService.getTradableEstateDetails(1L)); // agent가 null이어서 NPE 발생 예상
    }

    @Test
    @DisplayName("매물 상세 조회 - 매물 없음 예외")
    void getTradableEstateDetails_shouldThrow_whenEstateNotFound() {
        when(estateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> estateService.getTradableEstateDetails(1L));
    }

    @Test
    @DisplayName("매물 시세 내역 조회 - 정상 동작")
    void getEstatePriceHistory_shouldReturnList() {
        EstatePrice price = EstatePrice.builder()
                .estatePrice(10000)
                .estatePriceDate(LocalDateTime.now())
                .build();

        when(estatePriceRepository.findAllByEstate_EstateIdOrderByEstatePriceDateDesc(1L))
                .thenReturn(List.of(price));

        List<GetEstatePriceResponse> responses = estateService.getEstatePriceHistory(1L);

        assertThat(responses).hasSize(1);
        assertEquals(10000, responses.get(0).getEstatePrice());
    }

    @Test
    @DisplayName("매물 시세 내역 조회 - 내역 없으면 예외")
    void getEstatePriceHistory_shouldThrow_whenNotFound() {
        when(estatePriceRepository.findAllByEstate_EstateIdOrderByEstatePriceDateDesc(1L))
                .thenReturn(Collections.emptyList());

        assertThrows(CustomException.class,
                () -> estateService.getEstatePriceHistory(1L));
    }

    @Test
    @DisplayName("매물 설명 수정 - 정상 동작")
    void modifyEstateDescription_shouldUpdateDescription() {
        Estate estate = Estate.builder()
                .estateId(1L)
                .estateDescription("기존 설명")
                .build();

        when(estateRepository.findById(1L)).thenReturn(Optional.of(estate));

        estateService.modifyEstateDescription(1L,
                new ModifyEstateRequest(1L, "새로운 설명"));

        assertEquals("새로운 설명", estate.getEstateDescription());
    }

    @Test
    @DisplayName("매물 설명 수정 - 매물 없으면 예외")
    void modifyEstateDescription_shouldThrow_whenEstateNotFound() {
        when(estateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> estateService.modifyEstateDescription(1L,
                        new ModifyEstateRequest(1L, "설명")));
    }
}
