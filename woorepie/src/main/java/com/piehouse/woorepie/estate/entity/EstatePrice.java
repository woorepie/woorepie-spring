package com.piehouse.woorepie.estate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "estate_price")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString(exclude = "estate")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EstatePrice {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long estatePriceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estate_id", nullable = false)
    private Estate estate;

    @Column(nullable = false)
    private Long estatePrice;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime estatePriceDate;

}
