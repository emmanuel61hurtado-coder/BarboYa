package com.barboya.barboya.controller;

import com.barboya.barboya.dto.MerchantDtos;
import com.barboya.barboya.model.Merchant;
import com.barboya.barboya.model.Product;
import com.barboya.barboya.security.CurrentUser;
import com.barboya.barboya.service.MerchantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/merchants", "/api/merchants"})
public class MerchantsController {
    private final MerchantService merchants;

    public MerchantsController(MerchantService merchants) {
        this.merchants = merchants;
    }

    @PostMapping
    public Merchant create(@RequestBody MerchantDtos.CreateMerchantRequest req) {
        return merchants.create(CurrentUser.id(), req);
    }

    @GetMapping
    public List<Merchant> list() {
        return merchants.list();
    }

    @PostMapping("/{id}/products")
    public Product addProduct(@PathVariable UUID id, @RequestBody MerchantDtos.CreateProductRequest req) {
        return merchants.addProduct(id, req);
    }

    @GetMapping("/{id}/products")
    public List<Product> products(@PathVariable UUID id) {
        return merchants.products(id);
    }
}