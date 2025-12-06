package com.example.exchange.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class ExchangeController {

    @GetMapping("/convert")
    public Map<String, Object> convert(@RequestParam double value) {
        Random random = new Random();

        // Falha do tipo Error (10% de chance, demora 5s)
        if (random.nextDouble() < 0.1) {
            try {
                System.out.println("[FAILURE] Request 2 - Error fault triggered!");
                Thread.sleep(5000);
                throw new RuntimeException("Simulated conversion error!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        double rate = 1.0 / (5.0 + random.nextDouble()); // taxa entre 1/5 e 1/6
        double converted = value * rate;

        Map<String, Object> response = new HashMap<>();
        response.put("rate", rate);
        response.put("convertedValue", converted);
        return response;
    }
}