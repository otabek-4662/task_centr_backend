import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// Rate-limit probe: hammers POST /api/auth/login with bad credentials.
// Expected on the CURRENT backend: zero 429 responses (no limiter configured).
// If a limiter (e.g. Bucket4j) is added later, this same script must start
// showing 429s — that is the acceptance signal.

const limited = new Counter('rate_limited_429');

export const options = {
  vus: 200,
  duration: '30s',
  thresholds: {
    http_req_failed: [{ threshold: 'rate<1', abortOnFail: false }],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ name: 'probe', password: 'wrong-password' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  if (res.status === 429) limited.add(1);
  check(res, { 'answered (401 expected, 429 = limiter present)': (r) => r.status === 401 || r.status === 429 });
}

export function handleSummary(data) {
  const429 = data.metrics.rate_limited_429 ? data.metrics.rate_limited_429.values.count : 0;
  return {
    stdout: JSON.stringify(
      {
        total_requests: data.metrics.http_reqs.values.count,
        status_429_count: const429,
        verdict: const429 === 0 ? 'NO RATE LIMITER DETECTED' : 'RATE LIMITER PRESENT',
      },
      null,
      2,
    ),
  };
}
