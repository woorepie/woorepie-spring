package com.piehouse.woorepie.agent.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString(exclude = "agentPassword")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Agent {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long agentId;

    @Column(nullable = false, length = 10)
    private String agentName;

    @Column(nullable = false, length = 320, unique = true)
    private String agentEmail;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 250)
    private String agentPassword;

    @Column(nullable = false, length = 20, unique = true)
    private String agentPhoneNumber;

    @Column(nullable = false)
    private LocalDate agentDateOfBirth;

    @Column(length = 1000)
    private String agentIdentificationUrl;

    @Column(nullable = false, length = 1000)
    private String agentCertUrl;

    @Column(nullable = false, length = 30)
    private String businessName;

    @Column(nullable = false, length = 20)
    private String businessNumber;

    @Column(nullable = false, length = 20)
    private String businessPhoneNumber;

    @Column(nullable = false, length = 100)
    private String businessAddress;

    @Column(nullable = false, length = 1000)
    private String warrantUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime agentJoinDate;

    @Column(length = 100)
    private String agentKyc;

}