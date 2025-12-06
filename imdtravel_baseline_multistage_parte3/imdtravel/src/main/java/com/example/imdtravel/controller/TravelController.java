package com.example.imdtravel.controller;

import com.example.imdtravel.service.TravelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TravelController {

    private final TravelService travelService;

    public TravelController(TravelService travelService) {
        this.travelService = travelService;
    }

    /**
     * Exemplo de body para teste no postman:
     * {
     *   "flight": "LATAM123",
     *   "day": "2025-11-01",
     *   "user": "101",
     *   "ft": true
     * }
     *
     * ft = true  -> ativa tolerância a falhas (Parte 3)
     * ft = false -> comportamento normal (Parte 1.1 / 1.2)
     */
    @PostMapping("/buyTicket")
    public ResponseEntity<?> buyTicket(@RequestBody Map<String, Object> request) {
        try {
            boolean ft = false;
            if (request.get("ft") != null) {
                Object v = request.get("ft");
                if (v instanceof Boolean) ft = (Boolean) v;
                else ft = Boolean.parseBoolean(v.toString());
            }

            Map<String, Object> result = travelService.buyTicket(request, ft);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal error", "details", ex.getMessage()));
        }
    }
}