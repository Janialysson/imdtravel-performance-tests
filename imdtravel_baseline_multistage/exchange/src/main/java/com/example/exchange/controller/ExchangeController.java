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
        double rate = 1.0 / (5.0 + random.nextDouble()); // taxa entre 1/5 e 1/6
        double converted = value * rate;

        Map<String, Object> response = new HashMap<>();
        response.put("rate", rate);
        response.put("convertedValue", converted);
        return response;
    }
}