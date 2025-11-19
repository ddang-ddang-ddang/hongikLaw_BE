import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 5,
  duration: '30s',
};

const BASE_URL = 'https://ddangx3.site';
const JWT = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzaGluYWUxMDIzQG5hdmVyLmNvbSIsImlhdCI6MTc2MzUwNDkxOSwiZXhwIjoxNzYzNTA4NTE5LCJ1c2VySWQiOjF9.3WKzB40G6CTyq7XqL6jdbxxaQSdsHBvkbmTOimjshAs';  // 똑같이 사용
const CASE_ID = 23;

export default function () {
  const url = `${BASE_URL}/api/final/judge/${CASE_ID}`;

  const payload = JSON.stringify({
    votesA: 10,
    votesB: 20,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${JWT}`,
    },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
    // ApiResponse 안에 "JUDGMENT_IN_PROGRESS" 같은 문자열이 들어있다면 이렇게 체크 가능
    // 'body has IN_PROGRESS': (r) => String(r.body).includes('JUDGMENT_IN_PROGRESS'),
  });

  sleep(1);
}
