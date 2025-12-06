import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 20,
  duration: "30s",
};

export default function () {
  const url = "http://localhost:8084/api/buyTicket";

  const payload = JSON.stringify({
    flight: "LATAM123",
    day: "2025-11-01",
    user: "teste",
    ft: true // tolerância a falhas ativada
  });

  const params = {
    headers: { "Content-Type": "application/json" },
  };

  const res = http.post(url, payload, params);

  check(res, {
    "status OK or handled": (r) => r.status === 200 || r.status === 500,
  });

  sleep(1);
}
