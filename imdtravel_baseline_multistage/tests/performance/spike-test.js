import http from "k6/http";
import { check } from "k6";

export const options = {
  stages: [
    { duration: "5s", target: 10 },
    { duration: "2s", target: 200 }, // SPIKE
    { duration: "5s", target: 10 },
    { duration: "5s", target: 0 },
  ],
};

export default function () {
  const url = "http://localhost:8084/api/buyTicket";

  const payload = JSON.stringify({
    flight: "LATAM123",
    day: "2025-11-01",
    user: "teste",
    ft: false
  });

  const params = {
    headers: { "Content-Type": "application/json" },
  };

  const res = http.post(url, payload, params);

  check(res, { "status is 200": (r) => r.status === 200 });
}
