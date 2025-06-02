package com.piehouse.woorepie.notification.repository;

import com.piehouse.woorepie.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByCustomer_CustomerIdAndIsReadFalse(Long customerId);
    List<Notification> findByCustomer_CustomerIdOrderByCreatedAtDesc(Long customerId);
}
