import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../services/authApi.js'
import { useAuthStore } from '../stores/authStore.js'

const MESSAGES = {
  AUTH_INVALID_CREDENTIALS: '이메일 또는 비밀번호가 올바르지 않습니다.',
  AUTH_LOCKED: '계정이 잠겼습니다. 잠시 후 다시 시도해 주세요.',
  NETWORK_ERROR: '서버에 연결할 수 없습니다.',
}

function loginErrorMessage(err) {
  if (err.code === 'AUTH_INVALID_CREDENTIALS') {
    const left =
      err.remainingAttempts != null ? ` (남은 시도 ${err.remainingAttempts}회)` : ''
    return `${MESSAGES.AUTH_INVALID_CREDENTIALS}${left}`
  }
  if (err.code === 'AUTH_LOCKED') {
    const ms = new Date(err.lockedUntil).getTime() - Date.now()
    if (Number.isNaN(ms)) return MESSAGES.AUTH_LOCKED
    const minutes = Math.max(1, Math.ceil(ms / 60000))
    return `계정이 잠겼습니다. 약 ${minutes}분 후에 다시 시도해 주세요.`
  }
  return MESSAGES[err.code] ?? '로그인에 실패했습니다.'
}

export default function LoginPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      const res = await login(email, password)
      setAuth(res.member, res.accessToken)
      navigate('/documents')
    } catch (err) {
      setError(loginErrorMessage(err))
    }
  }

  return (
    <div className="mx-auto mt-24 max-w-sm p-6">
      <h1 className="mb-6 text-2xl font-bold">로그인</h1>
      <form onSubmit={handleSubmit} className="flex flex-col gap-3">
        <input
          className="rounded border p-2"
          type="email"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <input
          className="rounded border p-2"
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button className="rounded bg-gray-800 p-2 text-white">로그인</button>
      </form>
    </div>
  )
}