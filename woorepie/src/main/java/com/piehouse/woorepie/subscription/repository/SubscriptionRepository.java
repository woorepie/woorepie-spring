package com.piehouse.woorepie.subscription.repository;

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
}
