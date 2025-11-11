package com.PassBanned.API.controller;


import com.PassBanned.API.service.BannedPassengerService;
import com.PassBanned.API.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private BannedPassengerService passengerService;

    @Autowired
    private UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = Map.of(
             "passengers", Map.of(
                "total", passengerService.getTotalPassengers(),
                "active", passengerService.getActiveBans(),
                "critical", passengerService.getCriticalCases(),
                "recent", passengerService.getRecentBans(7).size()
            ),
            "users", Map.of(
                "total", userService.getTotalUsers(),
                "admins", userService.getAdminCount(),
                "active", userService.getActiveUserCount()
            )
        );
        return ResponseEntity.ok(stats);
    }
}