package com.barboya.barboya.repository;

import com.barboya.barboya.model.Order;
import com.barboya.barboya.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomer(User customer);
    List<Order> findByCourier(User courier);
}