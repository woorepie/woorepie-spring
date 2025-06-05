package com.piehouse.woorepie.customer.entity;

import com.piehouse.woorepie.estate.entity.Estate;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString(exclude = {"customer", "estate"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Account {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estate_id", nullable = false)
    private Estate estate;

    @Column(nullable = false)
    private Long accountTokenAmount;

    @Column(nullable = false)
    private Long totalAccountAmount;

    // 계좌 토큰 수량 업데이트 메소드
    public Account updateTokenAmount(long newTokenAmount) {
        this.accountTokenAmount = newTokenAmount;
        return this;
    }

    // 계좌 총액 업데이트 메소드
    public Account updateTotalAmount(long newTotalAmount) {
        this.totalAccountAmount = newTotalAmount;
        return this;
    }
}