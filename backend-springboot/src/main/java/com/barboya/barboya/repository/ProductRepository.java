package com.barboya.barboya.repository;

import com.barboya.barboya.model.Merchant;
import com.barboya.barboya.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByMerchant(Merchant merchant);
}