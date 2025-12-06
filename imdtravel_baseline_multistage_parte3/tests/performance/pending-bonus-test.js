import http from 'k6/http';
import { sleep, check } from 'k6';

export let options = {
  vus: 30,                    // 30 usuários simultâneos
  duration: '30s',            // 30 segundos contínuos
  thresholds: {
    http_req_failed: ['rate<0.10'],   // Até 10% permitido (debito normal por fila)
    http_req_duration: ['p(95)<1200'], // Até 1200ms, devido ao impacto da fila
  },
};

export default function () {
  const value = Math.floor(Math.random() * 3000) + 500;

  const res = http.get(`http://localhost:8080/api/travel/calculate?from=BR&to=US&value=${value}`);

  check(res, {
    "status válido": (r) => r.status === 200,
  });

  sleep(0.5);
}
