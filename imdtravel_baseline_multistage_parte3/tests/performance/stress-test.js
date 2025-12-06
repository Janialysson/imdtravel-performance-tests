import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '5s', target: 50 },
    { duration: '10s', target: 120 },
    { duration: '10s', target: 200 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.15'],    // até 15% tolerado
    http_req_duration: ['p(99)<1800'], // limite extremo
  }
};

export default function () {
  const res = http.get(
    "http://localhost:8080/api/travel/calculate?from=BR&to=US&value=1500"
  );

  check(res, {
    "resposta recebida": (r) => r.status === 200 || r.status === 500,
  });
}
