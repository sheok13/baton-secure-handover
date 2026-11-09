import { useAuthStore } from '../stores/authStore.js'

const BASE = import.meta.env.VITE_API_BASE_URL || '/api'

// 실패 응답을 던질 객체로 변환
function toError(status, body) {
  return { status, ...body?.error }
}

// 본문을 JSON으로 읽되, 실패하면 null (502 등은 본문이 HTML이거나 비어 있음)
async function readJson(res) {
  try {
    return await res.json()
  } catch {
    return null
  }
}

// ---- 토큰 재발급 ----
let refreshing = null // 진행 중인 재발급 Promise (동시에 여러 번 호출돼도 한 번만 요청)

async function doRefresh() {
  let res
  try {
    // request()를 쓰면 401 재시도와 서로 물려 무한 반복될 수 있어서 fetch를 직접 사용
    res = await fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      credentials: 'include', // 리프레시 쿠키 전송
    })
  } catch {
    useAuthStore.getState().clear()
    throw { status: 0, code: 'NETWORK_ERROR' }
  }

  const json = await readJson(res)
  if (!res.ok) {
    useAuthStore.getState().clear()
    throw toError(res.status, json)
  }

  // 재발급 응답은 { success, data: { accessToken, ... } } 로 가정 (응답 예시 확정 시 이 줄만 확인)
  const token = json?.data?.accessToken
  if (!token) {
    useAuthStore.getState().clear()
    throw { status: res.status, code: 'AUTH_REFRESH_INVALID' }
  }

  const { member, setAuth } = useAuthStore.getState()
  setAuth(member, token)
  return token
}

export function refresh() {
  if (!refreshing) {
    refreshing = doRefresh().finally(() => {
      refreshing = null
    })
  }
  return refreshing
}

// ---- 공통 요청 ----
export async function request(path, { method = 'GET', body, retry = false } = {}) {
  const headers = {}
  const isForm = body instanceof FormData

  // JSON일 때만 Content-Type 지정. FormData는 브라우저가 boundary를 붙여 자동으로 설정
  if (body && !isForm) headers['Content-Type'] = 'application/json'

  const token = useAuthStore.getState().accessToken
  if (token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body ? (isForm ? body : JSON.stringify(body)) : undefined,
      credentials: 'include',
    })
  } catch {
    throw { status: 0, code: 'NETWORK_ERROR' }
  }

  const json = await readJson(res)

  // 401이면 재발급 후 한 번만 다시 시도 (로그인 실패 401은 제외)
  if (res.status === 401 && !retry && json?.error?.code !== 'AUTH_INVALID_CREDENTIALS') {
    await refresh() // 실패하면 여기서 throw되고 스토어는 이미 clear됨
    return request(path, { method, body, retry: true })
  }

  if (!res.ok) throw toError(res.status, json)

  // { success, data, message } 전체를 돌려줌. 호출하는 쪽에서 res.data로 꺼내 씀
  return json
}