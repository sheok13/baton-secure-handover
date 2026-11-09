import { NavLink, useNavigate } from 'react-router-dom'
import { logout } from '../services/authApi.js'
import { useAuthStore } from '../stores/authStore.js'
import { DEPT_LABEL, RANK_LABEL } from '../constants/index.js'

const navClass = ({ isActive }) =>
  isActive ? 'font-bold text-white' : 'text-gray-400 hover:text-white'

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
    <header className="flex items-center justify-between border-b px-6 py-3">
      <div className="flex items-center gap-5">
        <span className="font-bold">Baton</span>
        <nav className="flex items-center gap-4 text-sm">
          <NavLink to="/documents" className={navClass}>
            문서함
          </NavLink>
          <NavLink to="/chat" className={navClass}>
            채팅
          </NavLink>
        </nav>
      </div>
      <div className="flex items-center gap-3">
        <span>
          {member?.name} · {DEPT_LABEL[member?.department]} {RANK_LABEL[member?.rank]}
        </span>
        <button className="rounded border px-3 py-1" onClick={handleLogout}>
          로그아웃
        </button>
      </div>
    </header>
  )
}