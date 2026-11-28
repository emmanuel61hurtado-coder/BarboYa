package com.barboya.barboya.repository;

import com.barboya.barboya.model.Subscription;
import com.barboya.barboya.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByDriver(User driver);
}