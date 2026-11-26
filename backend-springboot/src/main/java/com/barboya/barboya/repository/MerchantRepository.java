package com.barboya.barboya.repository;

import com.barboya.barboya.model.Merchant;
import com.barboya.barboya.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    List<Merchant> findByOwner(User owner);
}