package com.perfume.shop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "status", "UP",
                "message", "Perfume Shop API is running",
                "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
