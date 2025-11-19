import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 5,            // 동시에 때리는 가상 유저 수 (처음엔 5 정도로)
  duration: '30s',   // 30초 동안 반복
};

const BASE_URL = 'https://ddangx3.site';
const JWT = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzaGluYWUxMDIzQG5hdmVyLmNvbSIsImlhdCI6MTc2MzQ5ODU5OSwiZXhwIjoxNzYzNTAyMTk5LCJ1c2VySWQiOjF9.RymHNfwmP5ZfPwWfdwIfCs9bmD3MeZkN4boEGbcdyrk';  // ey... 로 시작하는 토큰
const CASE_ID = 23;  // 성능테스트용 사건 ID 하나 골라서 쓰면 됨

export default function () {
  const url = `${BASE_URL}/api/final/judge/${CASE_ID}`;

  // 실제 votesA/B 값은 네 서버 로직에 맞게 맞춰줘
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
  });

  // 너무 과하게 두드리는 거 방지
  sleep(1);
}
