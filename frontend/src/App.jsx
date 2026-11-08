import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import DocumentsPage from './pages/DocumentsPage.jsx'
import Toast from './components/Toast.jsx'
import { useAuthStore } from './stores/authStore.js'

// 임시(확인용, 커밋 전 삭제)
useAuthStore.getState().setAuth({ name: '테스트', department: 'DEV', rank: 'LEADER' }, 'mock-token')

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/documents" element={<DocumentsPage />} />
        <Route path="*" element={<Navigate to="/documents" replace />} />
      </Routes>
      <Toast />
    </BrowserRouter>
  )
}