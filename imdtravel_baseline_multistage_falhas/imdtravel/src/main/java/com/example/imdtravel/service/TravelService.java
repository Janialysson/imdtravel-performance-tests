package com.example.imdtravel.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Map;

@Service
public class TravelService {

    private RestTemplate restTemplate;

    public TravelService() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(1000);
        rf.setReadTimeout(1000);
        this.restTemplate = new RestTemplate(rf);
    }

    public String buyTicket(String flight, String day, int user) {
        try {
            String flightUrl = "http://airlineshub:8081/api/flight?flight=" + flight + "&day=" + day;
            Map flightData = restTemplate.getForObject(flightUrl, Map.class);

            String exchangeUrl = "http://exchange:8082/api/exchange";
            Double rate = restTemplate.getForObject(exchangeUrl, Double.class);

            double valueUsd = ((Number) flightData.get("value")).doubleValue();
            double valueBrl = valueUsd * rate;

            String sellUrl = "http://airlineshub:8081/api/sell?flight=" + flight + "&day=" + day;
            Map sellResp = restTemplate.postForObject(sellUrl, null, Map.class);
            String transactionId = (String) sellResp.get("transactionId");

            String fidelityUrl = "http://fidelity:8083/api/bonus?user=" + user + "&bonus=" + Math.round(valueUsd);
            restTemplate.postForObject(fidelityUrl, null, String.class);

            return "SUCCESS; transaction=" + transactionId + "; valueBRL=" + Math.round(valueBrl*100.0)/100.0;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
