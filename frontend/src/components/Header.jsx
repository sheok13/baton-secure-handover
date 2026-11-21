import { NavLink, useNavigate } from 'react-router-dom'
import { logout } from '../services/authApi.js'
import { useAuthStore } from '../stores/authStore.js'
import { DEPT_LABEL, RANK_LABEL } from '../constants/index.js'

const navClass = ({ isActive }) =>
  `inline-flex min-h-11 items-center rounded-lg px-3.5 text-[15px] ${
    isActive ? 'bg-brand-soft font-medium text-brand-dark' : 'text-[#3A4458] hover:bg-bg'
  }`

export default function Header() {
  const navigate = useNavigate()
  const member = useAuthStore((s) => s.member)
  const clear = useAuthStore((s) => s.clear)

  async function handleLogout() {
    try {
      await logout()
    } catch {
      // 서버 호출이 실패해도 화면의 로그인 상태는 정리한다
    }
    clear()
    navigate('/login')
  }

  return (
    <header className="flex flex-wrap items-center justify-between gap-3 border-b border-line bg-white px-10 py-2">
      <div className="flex flex-wrap items-center gap-6">
        <div className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M5 4h10l4 4v12H5z" />
              <path d="M9 13h6M9 17h4" />
            </svg>
          </div>
          <span className="text-lg font-bold">바톤</span>
        </div>
        <nav className="flex gap-1">
          <NavLink to="/documents" className={navClass}>
            문서함
          </NavLink>
          <NavLink to="/chat" className={navClass}>
            채팅
          </NavLink>
        </nav>
      </div>
      <div className="flex items-center gap-3">
        <span className="text-sm font-medium">
          {member?.name} {RANK_LABEL[member?.rank]}{' '}
          <span className="font-normal text-sub">· {DEPT_LABEL[member?.department]}</span>
        </span>
        <button
          type="button"
          className="inline-flex min-h-11 items-center rounded-lg px-3.5 text-sm text-sub hover:bg-bg"
          onClick={handleLogout}
        >
          로그아웃
        </button>
      </div>
    </header>
  )
}
