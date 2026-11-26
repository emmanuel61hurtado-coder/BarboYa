package com.barboya.barboya.controller;

import com.barboya.barboya.model.Order;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/orders", "/api/orders"})
public class OrdersController {
    @PostMapping
    public Order create(@RequestBody Object req) {
        // placeholder: keep minimal
        Order o = new Order();
        return o;
    }
}