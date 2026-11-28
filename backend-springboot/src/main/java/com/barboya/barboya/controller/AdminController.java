package com.barboya.barboya.controller;

import com.barboya.barboya.dto.AdminDtos;
import com.barboya.barboya.model.DriverProfile;
import com.barboya.barboya.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/admin", "/api/admin"})
public class AdminController {
    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @PutMapping("/drivers/billing")
    public void billing(@RequestBody AdminDtos.BillingUpdateRequest req) {
        admin.updateBilling(req);
    }

    @PostMapping("/subscriptions")
    public void subscription(@RequestParam UUID driverId, @RequestParam String plan) {
        admin.activateSubscription(driverId, plan);
    }

    @GetMapping("/drivers")
    public List<DriverProfile> drivers() {
        return admin.drivers();
    }
}