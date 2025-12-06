import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 20,               // 20 usuários simultâneos
  duration: "30s",       // 30 segundos de teste
};

export default function () {
  const url = "http://localhost:8084/api/buyTicket";

  const payload = JSON.stringify({
    flight: "LATAM123",
    day: "2025-11-01",
    user: "teste",
    ft: false // sem tolerância a falhas
  });

  const params = {
    headers: { "Content-Type": "application/json" },
  };

  const res = http.post(url, payload, params);

  check(res, {
    "status 200": (r) => r.status === 200,
  });

  sleep(1);
}
