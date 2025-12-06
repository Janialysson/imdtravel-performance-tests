package com.example.imdtravel.controller;

import com.example.imdtravel.service.TravelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TravelController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/buyTicket")
    public Map<String, Object> buyTicket(@RequestBody Map<String, Object> request) {

        String flight = request.get("flight").toString();
        String day = request.get("day").toString();
        String user = request.get("user").toString();

        // Consulta o preço do voo no AirlinesHub
        Map flightData = restTemplate.getForObject(
                "http://localhost:8083/flight?flight=" + flight + "&day=" + day, Map.class);

        double valueUSD = Double.parseDouble(flightData.get("value").toString());

        // Converte valor para BRL usando o Exchange
        Map exchangeData = restTemplate.getForObject(
                "http://localhost:8081/convert?value=" + valueUSD, Map.class);

        double convertedValue = Double.parseDouble(exchangeData.get("convertedValue").toString());

        // Efetiva venda no AirlinesHub
        Map<String, Object> sellRequest = new HashMap<>();
        sellRequest.put("flight", flight);
        sellRequest.put("user", user);
        Map sellResponse = restTemplate.postForObject("http://localhost:8083/sell", sellRequest, Map.class);

        // Adiciona bônus de fidelidade
        double bonus = convertedValue * 0.1; // 10% de bônus
        Map<String, Object> fidelityRequest = new HashMap<>();
        fidelityRequest.put("user", user);
        fidelityRequest.put("bonus", bonus);
        restTemplate.postForObject("http://localhost:8082/bonus", fidelityRequest, Map.class);

        // Retorna resposta final
        Map<String, Object> response = new HashMap<>();
        response.put("flight", flight);
        response.put("valueBRL", convertedValue);
        response.put("bonus", bonus);
        response.put("transaction", sellResponse.get("transactionId"));
        response.put("status", "Ticket purchased successfully!");
        return response;
    }
}
