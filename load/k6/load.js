import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const boardLatency = new Trend('board_latency');
const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '2m', target: 50 },
    { duration: '1m', target: 200 },
    { duration: '2m', target: 200 },
    { duration: '1m', target: 500 },
    { duration: '2m', target: 500 },
    { duration: '1m', target: 1000 },
    { duration: '2m', target: 1000 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: false }],
    http_req_duration: [{ threshold: 'p(95)<3000', abortOnFail: false }],
    board_latency: [{ threshold: 'p(95)<3000', abortOnFail: false }],
    errors: [{ threshold: 'rate<0.05', abortOnFail: false }],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const WS = __ENV.WS_ID || '6a45163133ff7819b28ef909';

export function setup() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ name: 'elshod', password: 'password123' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  if (res.status !== 200) throw new Error(`setup login failed: ${res.status}`);
  return { token: res.json('data.token') };
}

function H(data) {
  return { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${data.token}` } };
}

export default function (data) {
  const headers = H(data);

  group('board read', () => {
    const res = http.get(`${BASE_URL}/api/workspaces/${WS}/board`, headers);
    boardLatency.add(res.timings.duration);
    const ok = check(res, { 'board 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  group('columns read', () => {
    const res = http.get(`${BASE_URL}/api/workspaces/${WS}/columns`, headers);
    const ok = check(res, { 'columns 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  group('column reorder write', () => {
    const list = http.get(`${BASE_URL}/api/workspaces/${WS}/columns`, headers);
    if (list.status !== 200) {
      errorRate.add(true);
      return;
    }
    const cols = list.json('data').slice(0, 3);
    const body = cols.map((c, i) => ({ id: c.id, order: i + 1 }));
    const res = http.patch(`${BASE_URL}/api/workspaces/${WS}/columns`, JSON.stringify(body), headers);
    const ok = check(res, { 'reorder 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  sleep(0.2);
}

export function handleSummary(data) {
  return {
    stdout: JSON.stringify(
      {
        vus_max: data.metrics.vus_max.values.max,
        http_req_duration_p95: data.metrics.http_req_duration.values['p(95)'],
        board_latency_p95: data.metrics.board_latency.values['p(95)'],
        http_req_failed: data.metrics.http_req_failed.values.rate,
        errors: data.metrics.errors.values.rate,
        http_reqs: data.metrics.http_reqs.values.count,
      },
      null,
      2,
    ),
  };
}
