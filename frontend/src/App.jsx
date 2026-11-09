import { useEffect } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage.jsx'
import DocumentsPage from './pages/DocumentsPage.jsx'
import ChatPage from './pages/ChatPage.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Layout from './components/Layout.jsx'
import Toast from './components/Toast.jsx'
import { useAuthStore } from './stores/authStore.js'
import { refresh } from './services/apiClient.js'
import { me } from './services/authApi.js'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export default function App() {
  useEffect(() => {
    async function restore() {
      const { setAuth, clear, setReady } = useAuthStore.getState()
      try {
        if (USE_MOCK) return // mock은 복구할 세션이 없음
        await refresh() // 쿠키로 새 토큰 발급 → 스토어에 저장
        const member = await me() // 새 토큰으로 내 정보 조회
        setAuth(member, useAuthStore.getState().accessToken)
      } catch {
        clear() // 복구 실패 = 로그인 안 된 상태
      } finally {
        setReady() // 성공이든 실패든 마지막에 반드시
      }
    }
    restore()
  }, [])

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/documents" element={<DocumentsPage />} />
<<<<<<< HEAD
=======
          <Route path="/chat" element={<ChatPage />} />
>>>>>>> 687640e1b911b6a0d7b7148dc0d99e3ac415dffd
        </Route>
        <Route path="*" element={<Navigate to="/documents" replace />} />
      </Routes>
      <Toast />
    </BrowserRouter>
  )
}