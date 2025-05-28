package com.piehouse.woorepie.customer.repository;

import com.piehouse.woorepie.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCustomerEmail(String email);

    boolean existsByCustomerPhoneNumber(String phoneNumber);

    boolean existsByAccountNumber(String accountNumber);

    Optional<Customer> findByCustomerEmail(String email);

    @Modifying
    @Query("UPDATE Customer c SET c.accountBalance = c.accountBalance - :amount WHERE c.customerId = :customerId AND c.accountBalance >= :amount")
    int decreaseBalance(@Param("customerId") Long customerId, @Param("amount") int amount);
}
