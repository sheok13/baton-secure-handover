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
    <div className="flex min-h-screen flex-wrap items-stretch bg-bg">
      <div className="flex min-w-0 flex-[1_1_420px] flex-col justify-between gap-8 bg-ink p-16 text-white">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-[10px] bg-brand">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M5 4h10l4 4v12H5z" />
              <path d="M9 13h6M9 17h4" />
            </svg>
          </div>
          <span className="text-xl font-bold">바톤</span>
        </div>
        <div className="flex flex-col gap-4">
          <h1 className="text-[40px] font-bold leading-tight">
            문서는 한곳에,
            <br />
            답변은 내 권한 안에서
          </h1>
          <p className="text-base leading-relaxed text-[#C9D2E3]">
            부서와 직급별로 문서를 올리고, 내가 볼 수 있는 문서만 근거로 AI에게 물어보세요.
          </p>
        </div>
        <div className="text-[13px] text-[#A9B4CA]">Rookies 6기 미니 프로젝트</div>
      </div>
      <div className="flex min-w-0 flex-[1_1_480px] items-center justify-center p-12">
        <form onSubmit={handleSubmit} className="flex w-full max-w-sm flex-col gap-5">
          <div className="flex flex-col gap-1.5">
            <h2 className="text-[26px] font-bold">로그인</h2>
            <p className="text-sm text-sub">계정으로 로그인하세요</p>
          </div>
          <div className="flex flex-col gap-2">
            <label htmlFor="email" className="text-sm font-medium">
              이메일
            </label>
            <input
              id="email"
              className="h-12 rounded-lg border border-field bg-white px-3.5 text-[15px]"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="flex flex-col gap-2">
            <label htmlFor="pw" className="text-sm font-medium">
              비밀번호
            </label>
            <input
              id="pw"
              className="h-12 rounded-lg border border-field bg-white px-3.5 text-[15px]"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            {error && <span className="text-[13px] text-danger">{error}</span>}
          </div>
          <button className="flex h-12 items-center justify-center rounded-lg bg-brand text-base font-medium text-white hover:bg-brand-dark">
            로그인
          </button>
        </form>
      </div>
    </div>
  )
}
