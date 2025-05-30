package com.piehouse.woorepie.agent.dto;

import com.piehouse.woorepie.agent.entity.Agent;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
public class SessionAgent implements UserDetails {

    private final Long agentId;

    private final String agentName;

    private final String agentEmail;

    private final Collection<? extends GrantedAuthority> authorities;

    public static SessionAgent fromAgent(Agent agent) {
        return SessionAgent.builder()
                .agentId(agent.getAgentId())
                .agentName(agent.getAgentName())
                .agentEmail(agent.getAgentEmail())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_AGENT")))
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return agentEmail;
    }
}
