package com.example.airlineshub.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class AirlinesHubController {

    @GetMapping("/flight")
    public Map<String, Object> getFlight(@RequestParam String flight, @RequestParam String day) {
        Random random = new Random();
        double value = 500 + random.nextInt(1000); // preço entre 500 e 1500 dólares

        Map<String, Object> response = new HashMap<>();
        response.put("flight", flight);
        response.put("day", day);
        response.put("value", value);
        return response;
    }

    @PostMapping("/sell")
    public Map<String, Object> sellFlight(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("transactionId", "TX-" + System.currentTimeMillis());
        response.put("status", "success");
        return response;
    }
}