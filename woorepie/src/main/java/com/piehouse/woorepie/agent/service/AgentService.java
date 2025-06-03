package com.piehouse.woorepie.agent.service;

import com.piehouse.woorepie.agent.dto.request.CreateAgentRequest;
import com.piehouse.woorepie.agent.dto.request.LoginAgentRequest;
import com.piehouse.woorepie.agent.dto.response.AgentEstateListResponse;
import com.piehouse.woorepie.agent.dto.response.GetAgentResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AgentService {

    void loginAgent(LoginAgentRequest agentRequest, HttpServletRequest request);

    void logoutAgent(HttpServletRequest request);

    Boolean checkAgentEmail(String agentEmail);

    void createAgent(CreateAgentRequest agentRequest, HttpServletRequest request);

    GetAgentResponse getAgentInfo(Long agentId);

    List<AgentEstateListResponse> getEstatesByAgent(Long agentId);

    Boolean checkAgentPhoneNumber(String phoneNumber);

}
