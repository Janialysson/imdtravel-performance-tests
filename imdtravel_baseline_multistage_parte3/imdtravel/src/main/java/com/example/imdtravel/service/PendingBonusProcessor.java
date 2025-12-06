package com.example.imdtravel.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Queue;

@Component
public class PendingBonusProcessor {

    // Serviço principal que contém a fila de bônus pendentes
    private final TravelService travelService;
    // RestTemplate para reenviar os bônus ao Fidelity
    private final RestTemplate restTemplate = new RestTemplate();

    public PendingBonusProcessor(TravelService travelService) {
        this.travelService = travelService;
    }

    /**
     * Método agendado que roda a cada 30 segundos.
     *
     * Ele verifica se existem bônus pendentes (quando o serviço Fidelity falhou
     * durante o processamento da compra).
     *
     * Caso existam, tenta reenviar um por um.
     */
    @Scheduled(fixedDelay = 30000)
    public void processPending() {
        Queue<Map<String, Object>> queue = travelService.getPendingBonusesQueue();
        // Se estiver vazia, não há nada a fazer
        if (queue.isEmpty()) return;

        int size = queue.size();
        for (int i = 0; i < size; i++) {
            Map<String, Object> bonus = queue.poll();
            if (bonus == null) continue;
            try {
                // Monta a requisição HTTP em formato JSON
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(bonus, headers);
                // Envia para o serviço Fidelity novamente
                restTemplate.postForEntity("http://localhost:8082/bonus", request, Map.class);
                System.out.println("[PENDING] Bonus resent successfully for user " + bonus.get("user"));
            } catch (RestClientException ex) {
                // Se falhar novamente, recoloca na fila para tentar na próxima execução
                System.out.println("[PENDING] Failed to resend bonus for user " + bonus.get("user") + " - will retry later");
                queue.add(bonus);
            }
        }
    }
}
