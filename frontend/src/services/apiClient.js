import { useAuthStore } from '../stores/authStore.js'

// TODO(1): API 주소 기준값. VITE_API_BASE_URL을 읽고, 없으면 '/api'를 기본값으로.
//   이유: 리프레시 쿠키가 Path=/api/auth 라서 요청 주소가 /api/... 여야 쿠키가 따라 감.
//   힌트: Vite에서 환경변수는 import.meta.env.VITE_... 로 읽음.
const BASE = import.meta.env.VITE_API_BASE_URL || '/api'

// 실패 응답을 던질 객체로 변환
function toError(status, body) {
    return { status, ...body?.error }
  // TODO(2): { status, ...body?.error } 형태로 반환
  //   이유: LoginPage가 err.code, err.remainingAttempts 로 바로 읽게 하려는 것.
  //   remainingAttempts 위치가 확정돼도 이 한 곳만 고치면 됨.
  //   body가 null일 수 있으니 ?. 를 쓰는 이유도 생각해 보기.
}

export async function request(path, { method = 'GET', body } = {}) {
  const headers = {}
  if (body) headers['Content-Type'] = 'application/json'

const token = useAuthStore.getState().accessToken
if (token) headers.Authorization = `Bearer ${token}`

  // TODO(3): body가 있을 때만 Content-Type: application/json 헤더 추가
  //   이유: GET에는 본문이 없으니 불필요하고, 서버가 JSON으로 읽게 알려주는 용도.

  // TODO(4): useAuthStore.getState().accessToken 이 있으면 Authorization: `Bearer ${token}` 추가
  //   이유: 훅(useAuthStore())은 컴포넌트 안에서만 쓸 수 있어서, 일반 함수에서는 getState()로 읽음.

  let res
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'include',   // 쿠키(리프레시 토큰)를 주고받으려면 필요
    })
  } catch {
    throw { status: 0, code: 'NETWORK_ERROR' }
    // TODO(5): throw { status: 0, code: 'NETWORK_ERROR' }
    //   이유: 서버에 닿지도 못한 경우(네트워크 끊김). 화면이 "연결 오류"를 보여줄 수 있게.
  }

  // TODO(6): 응답 본문을 JSON으로 읽되, 실패하면 null
  //   이유: 게이트웨이 오류(502 등)는 본문이 HTML이거나 비어 있어서 json()이 예외를 던지고,
  //   그러면 진짜 상태 코드가 가려짐.
let json = null
try {
  json = await res.json()
} catch {
  // 본문이 없거나 JSON이 아니면 null 그대로 둔다
}

  if (!res.ok) throw toError(res.status, json)

  // TODO(7): 성공 시 무엇을 돌려줄지 결정: json 전체({success,data,message}) vs json.data
  //   힌트: 다운로드 성공 메시지("다운로드되었습니다.")는 message에 있음. 이 값이 필요한 호출이 있으니
  //   전체를 돌려주는 쪽이 유연함. 호출하는 쪽(authApi, documentApi)에서 res.data로 꺼내 쓰면 됨.
  return json
}