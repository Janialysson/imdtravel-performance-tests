package com.example.fidelity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
public class FidelityController {

    @PostMapping("/bonus")
    public Map<String, Object> giveBonus(@RequestBody Map<String, Object> data) {
        String user = data.get("user").toString();
        double bonus = Double.parseDouble(data.get("bonus").toString());

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("bonus", bonus);
        response.put("status", "Bonus added successfully!");
        return response;
    }
}
