package com.piehouse.woorepie.subscription.entity;

import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString(exclude = {"estate", "customer"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Subscription {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estate_id", nullable = false)
    private Estate estate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Long subTokenAmount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime subDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_status", nullable = false)
    private SubStatus subStatus = SubStatus.PENDING;

    // 상태 변경 메소드 추가
    public void changeStatus(SubStatus newStatus) {
        this.subStatus = newStatus;
    }

    // 토큰 수량 업데이트 메소드 추가
    public void changeSubTokenAmount(long newAmount) {
        this.subTokenAmount = newAmount;
    }

}
