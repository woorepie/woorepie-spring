package com.piehouse.woorepie.trade.entity;

import com.piehouse.woorepie.customer.entity.Customer;
import com.piehouse.woorepie.estate.entity.Estate;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString(exclude = {"estate", "seller", "buyer"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Trade {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estate_id", nullable = false)
    private Estate estate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Customer seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Customer buyer;

    @Column(nullable = false)
    private Long tokenPrice;

    @Column(nullable = false)
    private Long tradeTokenAmount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime tradeDate;

}