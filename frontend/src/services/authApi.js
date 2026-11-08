import { request } from './apiClient.js'
import { users } from '../mocks/data.js'
import { useAuthStore } from '../stores/authStore.js'

// TODO(1): .env 값은 문자열이라 'true'와 비교
const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function login(email, password) {
  if (USE_MOCK) {
    // TODO(2): email과 password가 둘 다 같은 사용자를 찾는다
    const user = users.find((u) => u.email === email && u.password === password)
    if (!user) {
      // 실 API 실패와 같은 모양으로 던져서 LoginPage가 구분 없이 처리하게 함
      throw { status: 401, code: 'AUTH_INVALID_CREDENTIALS', remainingAttempts: 3 }
    }
    // member에는 name, department, rank만 (email, password는 넣지 않음)
    return {
      accessToken: 'mock-token',
      member: { name: user.name, department: user.department, rank: user.rank },
    }
  }
  // TODO(3): 실 API. 응답은 { success, data: { accessToken, expiresIn, member } }이라 data만 반환
  const res = await request('/auth/login', { method: 'POST', body: { email, password } })
  return res.data
}

export async function me() {
  if (USE_MOCK) {
    // TODO(4): mock에는 서버가 없어서, 스토어에 member가 있을 때만 돌려줌
    const { member } = useAuthStore.getState()
    if (!member) throw { status: 401, code: 'AUTH_REFRESH_INVALID' }
    return member
  }
  const res = await request('/auth/me')
  return res.data
}

export async function logout() {
  // TODO(5): mock은 할 일이 없고, 실 API는 POST (응답 본문 없음, 204)
  if (USE_MOCK) return
  await request('/auth/logout', { method: 'POST' })
}