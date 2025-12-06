import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '10s', target: 20 },
    { duration: '20s', target: 50 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],      // Até 5% de falhas toleradas
    http_req_duration: ['p(95)<900'],   // 95% das requisições < 900ms
  }
};

export default function () {
  const res = http.get("http://localhost:8080/api/travel/calculate?from=BR&to=US&value=1000");

  check(res, {
    'status 200': (r) => r.status === 200,
    'resposta rápida (<900ms)': (r) => r.timings.duration < 900,
  });

  sleep(1);
}
