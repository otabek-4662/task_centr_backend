import http from 'k6/http';
import { check, group, sleep } from 'k6';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER = __ENV.K6_USER || 'k6smoke';
const PASS = __ENV.K6_PASS || 'password123';

function authHeaders(token) {
  return { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } };
}

export default function () {
  group('auth', () => {
    const reg = http.post(
      `${BASE_URL}/api/auth/register`,
      JSON.stringify({ name: `${USER}_${Date.now()}`, password: PASS }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    check(reg, { 'register 201': (r) => r.status === 201 });

    const login = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ name: 'elshod', password: 'password123' }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    check(login, { 'login 200 + token': (r) => r.status === 200 && r.json('data.token') !== '' });
  });

  const login = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ name: 'elshod', password: 'password123' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  const token = login.json('data.token');
  const H = authHeaders(token);
  const WS = '6a45163133ff7819b28ef909';

  group('read', () => {
    check(http.get(`${BASE_URL}/api/workspaces`, H), { 'workspaces 200': (r) => r.status === 200 });
    check(http.get(`${BASE_URL}/api/workspaces/${WS}/columns`, H), { 'columns 200': (r) => r.status === 200 });
    check(http.get(`${BASE_URL}/api/workspaces/${WS}/board`, H), { 'board 200': (r) => r.status === 200 });
    check(http.get(`${BASE_URL}/api/workspaces/${WS}/members`, H), { 'members 200': (r) => r.status === 200 });
  });

  group('write', () => {
    const col = http.post(
      `${BASE_URL}/api/workspaces/${WS}/columns`,
      JSON.stringify({ title: `smoke-${Date.now()}` }),
      H,
    );
    check(col, { 'column created': (r) => r.status === 200 && r.json('data.order') >= 1 });
    const colId = col.json('data.id');

    const task = http.post(
      `${BASE_URL}/api/workspaces/${WS}/tasks`,
      JSON.stringify({ title: 'smoke-task', columnId: colId }),
      H,
    );
    check(task, { 'task created': (r) => r.status === 200 });
    const taskId = task.json('data.id');

    const moved = http.patch(
      `${BASE_URL}/api/workspaces/${WS}/tasks/${taskId}`,
      JSON.stringify({ order: 5 }),
      H,
    );
    check(moved, { 'task patched': (r) => r.status === 200 && r.json('data.order') === 5 });

    const del = http.del(`${BASE_URL}/api/workspaces/${WS}/tasks/${taskId}`, null, H);
    check(del, { 'task deleted': (r) => r.status === 200 });
    const delCol = http.del(`${BASE_URL}/api/workspaces/${WS}/columns/${colId}`, null, H);
    check(delCol, { 'column deleted': (r) => r.status === 200 });
  });

  sleep(1);
}
