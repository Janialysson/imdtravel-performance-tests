package com.example.fidelity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class FidelityController {

    @PostMapping("/bonus")
    public Map<String, Object> giveBonus(@RequestBody Map<String, Object> data) {
        Random random = new Random();

        // Falha Crash (2% de chance)
        if (random.nextDouble() < 0.02) {
            System.out.println("[FAILURE] Request 4 - Crash fault triggered!");
            System.exit(1); // encerra o processo
        }

        String user = data.get("user").toString();
        int bonus = (int) Math.round(Double.parseDouble(data.get("bonus").toString()));

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("bonus", bonus);
        response.put("status", "Bonus added successfully!");
        return response;
    }
}
