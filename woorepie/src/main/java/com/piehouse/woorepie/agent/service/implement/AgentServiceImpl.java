package com.piehouse.woorepie.agent.service.implement;

import com.piehouse.woorepie.agent.dto.SessionAgent;
import com.piehouse.woorepie.agent.dto.request.CreateAgentRequest;
import com.piehouse.woorepie.agent.dto.request.LoginAgentRequest;
import com.piehouse.woorepie.agent.dto.response.AgentEstateListResponse;
import com.piehouse.woorepie.agent.dto.response.GetAgentResponse;
import com.piehouse.woorepie.agent.entity.Agent;
import com.piehouse.woorepie.agent.repository.AgentRepository;
import com.piehouse.woorepie.agent.service.AgentService;
import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstatePrice;
import com.piehouse.woorepie.estate.repository.DividendRepository;
import com.piehouse.woorepie.estate.repository.EstatePriceRepository;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.estate.service.EstateRedisService;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.global.service.implement.S3ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;
    private final EstateRepository estateRepository;
    private final EstatePriceRepository estatePriceRepository;
    private final DividendRepository dividendRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3ServiceImpl s3Service;
    private final EstateRedisService estateRedisService;

    @Override
    @Transactional(readOnly = true)
    public void loginAgent(LoginAgentRequest agentRequest, HttpServletRequest request) {
        Agent agent = agentRepository.findByAgentEmail(agentRequest.getAgentEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(agentRequest.getAgentPassword(), agent.getAgentPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!agent.getAgentPhoneNumber().equals(agentRequest.getAgentPhoneNumber())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        SessionAgent principal = SessionAgent.fromAgent(agent);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(auth);

        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

    }

    @Override
    public void logoutAgent(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                authentication.getAuthorities().stream()
                        .noneMatch(auth -> auth.getAuthority().equals("ROLE_AGENT"))) {
            throw new AccessDeniedException("로그아웃은 AGENT 권한을 가진 사용자만 가능합니다.");
        }

        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
    }

    @Override
    public Boolean checkAgentEmail(String agentEmail) {

        if (agentRepository.existsByAgentEmail(agentEmail)) {
            throw new CustomException(ErrorCode.ALREADY_REGISTERED_EMAIL);
        }

        return true;
    }

    @Override
    @Transactional
    public void createAgent(CreateAgentRequest agentRequest, HttpServletRequest request) {
        if (agentRepository.existsByAgentEmail(agentRequest.getAgentEmail()) ||
                agentRepository.existsByAgentPhoneNumber(agentRequest.getAgentPhoneNumber())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }

        Agent agent = Agent.builder()
                .agentName(agentRequest.getAgentName())
                .agentEmail(agentRequest.getAgentEmail())
                .agentPassword(passwordEncoder.encode(agentRequest.getAgentPassword()))
                .agentPhoneNumber(agentRequest.getAgentPhoneNumber())
                .agentDateOfBirth(agentRequest.getAgentDateOfBirth())
                .agentIdentificationUrl(s3Service.getPublicS3Url(agentRequest.getAgentIdentificationUrlKey()))
                .agentCertUrl(s3Service.getPublicS3Url(agentRequest.getAgentCertUrlKey()))
                .businessName(agentRequest.getBusinessName())
                .businessNumber(agentRequest.getBusinessNumber())
                .businessPhoneNumber(agentRequest.getBusinessPhoneNumber())
                .businessAddress(agentRequest.getBusinessAddress())
                .warrantUrl(s3Service.getPublicS3Url(agentRequest.getWarrantUrlKey()))
                .agentKyc(UUID.randomUUID().toString())
                .build();
        agentRepository.save(agent);

    }

    // agent 정보 조회
    @Override
    @Transactional(readOnly = true)
    public GetAgentResponse getAgentInfo(Long agentId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return GetAgentResponse.builder()
                .agentName(agent.getAgentName())
                .agentPhoneNumber(agent.getAgentPhoneNumber())
                .agentEmail(agent.getAgentEmail())
                .agentDateOfBirth(agent.getAgentDateOfBirth())
                .agentCertUrl(agent.getAgentCertUrl())
                .businessName(agent.getBusinessName())
                .businessNumber(agent.getBusinessNumber())
                .businessAddress(agent.getBusinessAddress())
                .businessPhoneNumber(agent.getBusinessPhoneNumber())
                .warrantUrl(agent.getWarrantUrl())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentEstateListResponse> getEstatesByAgent(Long agentId) {
        // 1. DB에서 agent의 estate 목록 조회
        List<Estate> estateList = estateRepository.findByAgent_AgentId(agentId);

        // 2. estateId만 리스트로 추출
        List<Long> estateIds = estateList.stream()
                .map(Estate::getEstateId)
                .toList();
        // 3. Redis에서 estateId 리스트로 가격 정보 한꺼번에 조회
        Map<Long, RedisEstatePrice> estatePriceMap = estateRedisService.getMultipleRedisEstatePrice(estateIds);

        // 4. estateList를 돌면서 각 estate에 price를 할당해서 응답 생성
        return estateList.stream()
                .map(e -> {
                    RedisEstatePrice price = estatePriceMap.get(e.getEstateId());
                    long estateTokenPrice = price != null ? price.getEstateTokenPrice() : 0; // int로 바로 할당
                    BigDecimal dividend = price != null ? price.getDividendYield() : BigDecimal.ZERO; // int로 바로 할당

                    assert price != null;
                    return AgentEstateListResponse.builder()
                            .estateId(e.getEstateId())
                            .estateName(e.getEstateName())
                            .tokenAmount(e.getTokenAmount())
                            .estateTokenPrice(estateTokenPrice)
                            .dividendYield(dividend)
                            .estateStatus(e.getEstateStatus().name())
                            .build();
                })
                .toList();
    }

    //전화번호 중복 확인
    @Override
    @Transactional(readOnly = true)
    public Boolean checkAgentPhoneNumber(String phoneNumber) {
        if (agentRepository.existsByAgentPhoneNumber(phoneNumber)) {
            throw new CustomException(ErrorCode.ALREADY_REGISTERED_PHONE);
        }
        return true;
    }

}
