package com.example.imdtravel.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import jakarta.annotation.PostConstruct;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Deque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.time.Instant;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class TravelService {

    // RestTemplate principal para requisições normais sem timeout
    private RestTemplate restTemplateDefault;

    // RestTemplate configurado com timeout de 2 segundos para o SELL
    private RestTemplate restTemplateSell;

    // Cache para guardar o último voo bem-sucedido (fallback do Request 1)
    // Key: "flight::day"  → Value: JSON com informações do voo
    private final ConcurrentMap<String, Map<String, Object>> lastFlightCache = new ConcurrentHashMap<>();

    // Guarda as últimas 10 taxas de conversão (fallback do Request 2)
    private final Deque<Double> lastRates = new ConcurrentLinkedDeque<>();

    // Fila para armazenar bônus que não puderam ser enviados ao Fidelity (Request 4)
    private final Queue<Map<String, Object>> pendingBonuses = new ConcurrentLinkedQueue<>();

    // Configurações do sistema
    private final int FLIGHT_RETRY = 3;       // Quantidade de tentativas no Request 1
    private final int SELL_TIMEOUT_MS = 2000; // Timeout de 2s para o Sell, Request 3
    private final int RATE_CACHE_MAX = 10;    // Máx. de taxas mantidas no cache

    @PostConstruct
    public void init() {
        // RestTemplate comum sem timeout
        this.restTemplateDefault = new RestTemplate();

        // Configuração do RestTemplate com timeout para o SELL
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(SELL_TIMEOUT_MS);
        factory.setReadTimeout(SELL_TIMEOUT_MS);
        this.restTemplateSell = new RestTemplate(factory);
    }

    /**
     * Método principal que orquestra toda a compra da passagem.
     * Ele realiza as requisições: flight → exchange → sell → fidelity
     *
     * @param request JSON recebido do cliente contendo: flight, day, user
     * @param ft  TRUE ativa mecanismos de tolerância a falhas (Parte 3)
     */
    public Map<String, Object> buyTicket(Map<String, Object> request, boolean ft) {

        // Verificação dos parâmetros obrigatórios
        if (!request.containsKey("flight") || !request.containsKey("day") || !request.containsKey("user")) {
            throw new IllegalArgumentException("Missing required fields: flight, day, user");
        }

        String flight = request.get("flight").toString();
        String day    = request.get("day").toString();
        String user   = request.get("user").toString();


        // 1) REQUEST 1 — Consulta ao AirlinesHub
        // Se ft=true → retry + fallback
        // Se ft=false → chama apenas 1 vez (comportamento visto na Parte 1.1)
        Map<String, Object> flightData = null;

        if (ft) {
            flightData = getFlightWithRetryAndFallback(flight, day);
        } else {
            flightData = callFlightService(flight, day);

            // Se conseguiu consultar, salva para cache (usado como fallback)
            if (flightData != null) {
                cacheFlight(flight, day, flightData);
            }
        }

        // Se nenhum dado de voo foi obtido → Então erro
        if (flightData == null) {
            return Map.of("status", "ERROR", "message", "Unable to obtain flight information");
        }

        double valueUSD = Double.parseDouble(flightData.get("value").toString());

        // REQUEST 2 — Conversão Exchange
        // Estratégias:
        // - ft=false → apenas chama o Exchange
        // - ft=true → usa média das últimas 10 taxas se Exchange falhar
        Double convertedValue = null;

        try {
            Map<String, Object> exchange = callExchangeService(valueUSD);

            if (exchange != null && exchange.containsKey("convertedValue")) {
                convertedValue = Double.parseDouble(exchange.get("convertedValue").toString());

                // Armazena a taxa no cache (para fallback)
                if (exchange.containsKey("rate")) {
                    addRateToCache(Double.parseDouble(exchange.get("rate").toString()));
                }
            }
        } catch (Exception e) {
            convertedValue = null;
        }

        // Se Exchange falhou então:
        if (convertedValue == null) {
            if (ft) {
                // Usa fallback da média das taxas armazenadas
                double fallbackRate = computeAverageRateFallback();
                convertedValue = valueUSD * fallbackRate;
            } else {
                return Map.of("status", "ERROR", "message", "Exchange service failed");
            }
        }


        // 3) REQUEST 3 — SELL com Timeout de 2s
        Map<String, Object> sellResponse = null;

        try {
            sellResponse = callSellServiceWithTimeout(flight, user);
        } catch (Exception e) {
            // ft=true →  para falha rápida
            if (ft) {
                return Map.of("status", "ERROR", "message", "sell service timeout or error");
            } else {
                return Map.of("status", "ERROR", "message", "sell service error: " + e.getMessage());
            }
        }


        // REQUEST 4 — Fidelity (pode crashar)

        // Se falhar, o bônus vai para fila pendente
        double bonus = Math.round(convertedValue * 0.10); // 10% de bônus
        Map<String, Object> bonusReq = Map.of("user", user, "bonus", (int) bonus);

        boolean fidelityOk = true;

        try {
            callFidelityService(bonusReq);
        } catch (Exception e) {
            fidelityOk = false;
            pendingBonuses.add(new HashMap<>(bonusReq)); // armazena para reenvio
        }


        // 5) Construção da Resposta Final
        Map<String, Object> response = new HashMap<>();
        response.put("flight", flight);
        response.put("day", day);
        response.put("originalValueUSD", valueUSD);
        response.put("valueBRL", convertedValue);
        response.put("transactionId", sellResponse != null ? sellResponse.get("transactionId") : "UNKNOWN");
        response.put("bonus", (int) bonus);
        response.put("fidelityQueued", !fidelityOk); // indica se ficou pendente
        response.put("status", "Purchase completed");
        response.put("timestamp", Instant.now().toString());

        return response;
    }

    // REQUEST 1 — Retry + Fallback
    private Map<String, Object> getFlightWithRetryAndFallback(String flight, String day) {

        String key = keyForFlight(flight, day);
        Map<String, Object> flightData = null;

        int attempts = 0;

        // Realiza até 3 tentativas
        while (attempts < FLIGHT_RETRY) {
            attempts++;
            try {
                flightData = callFlightService(flight, day);

                if (flightData != null) {
                    cacheFlight(flight, day, flightData);
                    return flightData;
                }

            } catch (Exception e) {
                // ignora erro e tenta novamente
            }
        }

        // Fallback final: retornar último valor conhecido
        Map<String, Object> fallback = lastFlightCache.get(key);

        if (fallback != null) {
            return new HashMap<>(fallback); // retorna a cópia
        }

        return null; // nenhum dado disponível
    }

    private Map<String, Object> callFlightService(String flight, String day) {
        String url = "http://localhost:8081/flight?flight=" + encode(flight) + "&day=" + encode(day);

        try {
            return restTemplateDefault.getForObject(url, Map.class);
        } catch (RestClientException rce) {
            return null;
        }
    }

    private void cacheFlight(String flight, String day, Map<String, Object> flightData) {
        String key = keyForFlight(flight, day);
        lastFlightCache.put(key, new HashMap<>(flightData));
    }

    private String keyForFlight(String flight, String day) {
        return flight + "::" + day;
    }


    // REQUEST 2 — Exchange com fallback (média das últimas 10 taxas)
    private Map<String, Object> callExchangeService(double valueUSD) {
        String url = "http://localhost:8082/convert?value=" + valueUSD;
        return restTemplateDefault.getForObject(url, Map.class);
    }

    // adiciona taxa ao histórico
    private synchronized void addRateToCache(double rate) {
        lastRates.addLast(rate);

        // mantém só 10 elementos
        while (lastRates.size() > RATE_CACHE_MAX) {
            lastRates.removeFirst();
        }
    }

    // fallback se Exchange falhar
    private double computeAverageRateFallback() {
        List<Double> copy = new ArrayList<>(lastRates);

        if (copy.isEmpty()) {
            return 1.0 / 5.5; // valor seguro padrão
        }

        return copy.stream().mapToDouble(d -> d).average().orElse(1.0 / 5.5);
    }


    // REQUEST 3 — SELL com timeout de 2 segundos (fail-fast)
    private Map<String, Object> callSellServiceWithTimeout(String flight, String user) {

        String url = "http://localhost:8081/sell";

        Map<String, Object> body = new HashMap<>();
        body.put("flight", flight);
        body.put("user", user);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> resp = restTemplateSell.postForEntity(url, request, Map.class);
            return resp.getBody();

        } catch (ResourceAccessException rae) {
            // timeout ou falha de conexão
            throw new RuntimeException("Sell service timeout or unavailable");

        } catch (HttpStatusCodeException hsce) {
            throw new RuntimeException("Sell service error: " + hsce.getStatusCode());
        }
    }

    // REQUEST 4 — Envio de bônus ao Fidelity + fallback para fila offline
    private void callFidelityService(Map<String, Object> bonusReq) {
        String url = "http://localhost:8083/bonus";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(bonusReq, headers);

        restTemplateDefault.postForEntity(url, request, Map.class);
    }

    // usada pelo PendingBonusProcessor
    public Queue<Map<String, Object>> getPendingBonusesQueue() {
        return pendingBonuses;
    }

    // Função auxiliar para codificar parâmetros na URL
    private String encode(String s) {
        return s.replace(" ", "%20");
    }
}