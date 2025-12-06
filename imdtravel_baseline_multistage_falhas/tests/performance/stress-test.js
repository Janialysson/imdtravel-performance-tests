import http from "k6/http";
import { check } from "k6";

export const options = {
  stages: [
    { duration: "20s", target: 30 },
    { duration: "20s", target: 70 },
    { duration: "20s", target: 0 },
  ],
};

export default function () {
  const url = "http://localhost:8084/api/buyTicket";

  const payload = JSON.stringify({
    flight: "LATAM123",
    day: "2025-11-01",
    user: "teste",
    ft: true
  });

  const params = {
    headers: { "Content-Type": "application/json" },
  };

  const res = http.post(url, payload, params);

  check(res, {
    "status 200 or tolerated": (r) =>
      r.status === 200 ||
      r.status === 500 ||
      r.status === 400,
  });
}
