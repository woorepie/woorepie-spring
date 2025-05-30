package com.piehouse.woorepie.subscription.repository;

import com.piehouse.woorepie.subscription.entity.SubStatus;
import com.piehouse.woorepie.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("select s from Subscription s join fetch s.estate where s.customer.customerId = :customerId")
    List<Subscription> findByCustomerIdWithEstate(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(s.subTokenAmount), 0) FROM Subscription s WHERE s.estate.estateId = :estateId")
    int sumSubTokenAmountByEstateId(@Param("estateId") Long estateId);

    // 해당 매물의 특정 상태 청약 내역을 신청일 기준 오름차순 조회
    List<Subscription> findAllByEstate_EstateIdAndSubStatusOrderBySubDateAsc(Long estateId, SubStatus subStatus);

    // 해당 매물의 특정 상태 청약 내역 전체 조회
    List<Subscription> findAllByEstate_EstateIdAndSubStatus(Long estateId, SubStatus subStatus);
}
