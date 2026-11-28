package com.barboya.barboya.controller;

import com.barboya.barboya.dto.DriversDtos;
import com.barboya.barboya.model.DriverProfile;
import com.barboya.barboya.security.CurrentUser;
import com.barboya.barboya.service.DriversService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/drivers", "/api/drivers"})
public class DriversController {
    private final DriversService drivers;

    public DriversController(DriversService drivers) {
        this.drivers = drivers;
    }

    @PostMapping("/location")
    public DriverProfile location(@RequestBody DriversDtos.LocationUpdate req) {
        return drivers.setLocation(CurrentUser.id(), req);
    }

    @PostMapping("/online")
    public DriverProfile online(@RequestBody DriversDtos.OnlineToggle req) {
        return drivers.setOnline(CurrentUser.id(), req.online());
    }
}