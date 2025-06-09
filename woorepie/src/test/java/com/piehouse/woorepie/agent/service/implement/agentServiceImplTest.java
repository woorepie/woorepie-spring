package com.piehouse.woorepie.agent.service.implement;

import com.piehouse.woorepie.agent.dto.request.CreateAgentRequest;
import com.piehouse.woorepie.agent.dto.request.LoginAgentRequest;
import com.piehouse.woorepie.agent.dto.response.AgentEstateListResponse;
import com.piehouse.woorepie.agent.dto.response.GetAgentResponse;
import com.piehouse.woorepie.agent.entity.Agent;
import com.piehouse.woorepie.agent.repository.AgentRepository;
import com.piehouse.woorepie.estate.dto.RedisEstatePrice;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import com.piehouse.woorepie.estate.repository.DividendRepository;
import com.piehouse.woorepie.estate.repository.EstatePriceRepository;
import com.piehouse.woorepie.estate.repository.EstateRepository;
import com.piehouse.woorepie.estate.service.EstateRedisService;
import com.piehouse.woorepie.global.exception.CustomException;
import com.piehouse.woorepie.global.exception.ErrorCode;
import com.piehouse.woorepie.global.service.implement.S3ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplTest {

    @Mock private AgentRepository agentRepository;
    @Mock private EstateRepository estateRepository;
    @Mock private EstatePriceRepository estatePriceRepository;
    @Mock private DividendRepository dividendRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private S3ServiceImpl s3Service;
    @Mock private EstateRedisService estateRedisService;

    @InjectMocks private AgentServiceImpl agentService;

    @Mock private HttpServletRequest httpServletRequest;
    @Mock private HttpSession httpSession;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("이메일 중복 확인 - 존재하지 않으면 true")
    void checkAgentEmail_shouldReturnTrue_whenNotExists() {
        when(agentRepository.existsByAgentEmail("test@agent.com")).thenReturn(false);
        Boolean result = agentService.checkAgentEmail("test@agent.com");
        assertTrue(result);
    }

    @Test
    @DisplayName("이메일 중복 확인 - 존재하면 예외")
    void checkAgentEmail_shouldThrow_whenExists() {
        when(agentRepository.existsByAgentEmail("test@agent.com")).thenReturn(true);
        assertThrows(CustomException.class, () -> agentService.checkAgentEmail("test@agent.com"));
    }

    @Test
    @DisplayName("전화번호 중복 확인 - 존재하지 않으면 true")
    void checkAgentPhoneNumber_shouldReturnTrue_whenNotExists() {
        when(agentRepository.existsByAgentPhoneNumber("01011112222")).thenReturn(false);
        Boolean result = agentService.checkAgentPhoneNumber("01011112222");
        assertTrue(result);
    }

    @Test
    @DisplayName("전화번호 중복 확인 - 존재하면 예외")
    void checkAgentPhoneNumber_shouldThrow_whenExists() {
        when(agentRepository.existsByAgentPhoneNumber("01011112222")).thenReturn(true);
        assertThrows(CustomException.class, () -> agentService.checkAgentPhoneNumber("01011112222"));
    }

    @Test
    @DisplayName("에이전트 로그인 성공 - 세션 저장")
    void loginAgent_shouldSetSession_whenValid() {
        Agent agent = Agent.builder()
                .agentEmail("test@agent.com")
                .agentPassword("encoded")
                .agentPhoneNumber("01011112222")
                .build();

        when(agentRepository.findByAgentEmail("test@agent.com")).thenReturn(Optional.of(agent));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(httpServletRequest.getSession(true)).thenReturn(httpSession);

        agentService.loginAgent(new LoginAgentRequest("test@agent.com", "password", "01011112222"), httpServletRequest);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("에이전트 로그인 실패 - 비밀번호 불일치")
    void loginAgent_shouldThrow_whenInvalidPassword() {
        Agent agent = Agent.builder()
                .agentEmail("test@agent.com")
                .agentPassword("encoded")
                .agentPhoneNumber("01011112222")
                .build();

        when(agentRepository.findByAgentEmail("test@agent.com")).thenReturn(Optional.of(agent));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(CustomException.class,
                () -> agentService.loginAgent(
                        new LoginAgentRequest("test@agent.com", "wrong", "01011112222"), httpServletRequest));
    }

    @Test
    @DisplayName("에이전트 생성")
    void createAgent_shouldCreateAgent() {
        CreateAgentRequest req = new CreateAgentRequest(
                "홍길동",
                "test@agent.com",
                "password",
                "01011112222",
                LocalDate.of(1990, 1, 1),
                "idUrlKey",
                "certUrlKey",
                "회사명",
                "1234567890",
                "0212345678",
                "서울특별시",
                "warrantKey"
        );

        when(agentRepository.existsByAgentEmail("test@agent.com")).thenReturn(false);
        when(agentRepository.existsByAgentPhoneNumber("01011112222")).thenReturn(false);
        when(s3Service.getPublicS3Url(any())).thenReturn("https://s3.url");

        agentService.createAgent(req, httpServletRequest);
        verify(agentRepository).save(any());
    }

    @Test
    @DisplayName("에이전트 정보 조회")
    void getAgentInfo_shouldReturnAgentResponse() {
        Long id = 1L;
        Agent agent = Agent.builder()
                .agentId(id)
                .agentName("홍길동")
                .agentEmail("test@agent.com")
                .agentPhoneNumber("01011112222")
                .agentDateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        when(agentRepository.findById(id)).thenReturn(Optional.of(agent));
        GetAgentResponse response = agentService.getAgentInfo(id);
        assertEquals("홍길동", response.getAgentName());
    }

    @Test
    @DisplayName("에이전트 부동산 목록 조회")
    void getEstatesByAgent_shouldReturnEstateListResponse() {
        Long agentId = 1L;
        Estate estate = Estate.builder()
                .estateId(1L)
                .estateName("빌딩A")
                .estateStatus(EstateStatus.SUCCESS)
                .tokenAmount(100)
                .build();

        when(estateRepository.findByAgent_AgentId(agentId)).thenReturn(List.of(estate));
        RedisEstatePrice price = RedisEstatePrice.builder()
                .estateTokenPrice(1000)
                .dividendYield(BigDecimal.TEN)
                .build();
        when(estateRedisService.getMultipleRedisEstatePrice(any()))
                .thenReturn(Map.of(1L, price));

        List<AgentEstateListResponse> result = agentService.getEstatesByAgent(agentId);
        assertThat(result).isNotEmpty();
    }
}