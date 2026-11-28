package com.barboya.barboya.service;

import com.barboya.barboya.dto.MerchantDtos;
import com.barboya.barboya.enums.Enums;
import com.barboya.barboya.model.Merchant;
import com.barboya.barboya.model.Product;
import com.barboya.barboya.model.User;
import com.barboya.barboya.repository.MerchantRepository;
import com.barboya.barboya.repository.ProductRepository;
import com.barboya.barboya.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MerchantService {
    private final MerchantRepository merchants;
    private final ProductRepository products;
    private final UserRepository users;

    public MerchantService(MerchantRepository merchants, ProductRepository products, UserRepository users) {
        this.merchants = merchants;
        this.products = products;
        this.users = users;
    }

    public Merchant create(UUID ownerId, MerchantDtos.CreateMerchantRequest req) {
        User owner = users.findById(ownerId).orElseThrow();
        Merchant m = new Merchant();
        m.setOwner(owner);
        m.setName(req.name());
        m.setAddress(req.address());
        m.setLat(req.lat());
        m.setLng(req.lng());
        try { m.setCategory(Enums.MerchantCategory.valueOf(req.category())); } catch (Exception e) { m.setCategory(Enums.MerchantCategory.other); }
        return merchants.save(m);
    }

    public List<Merchant> list() { return merchants.findAll(); }

    public Product addProduct(UUID merchantId, MerchantDtos.CreateProductRequest req) {
        Merchant m = merchants.findById(merchantId).orElseThrow();
        Product p = new Product();
        p.setMerchant(m);
        p.setName(req.name());
        p.setDescription(req.description());
        p.setPriceCop(req.priceCop());
        return products.save(p);
    }

    public List<Product> products(UUID merchantId) {
        Merchant m = merchants.findById(merchantId).orElseThrow();
        return products.findByMerchant(m);
    }
}