import { useEffect } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage.jsx'
import DocumentsPage from './pages/DocumentsPage.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Toast from './components/Toast.jsx'
import { useAuthStore } from './stores/authStore.js'

export default function App() {
  useEffect(() => {
    // TODO: 복구(refresh → me) 확정 후 교체. 지금은 준비 완료로만 표시
    useAuthStore.getState().setReady()
  }, [])

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/documents"
          element={
            <ProtectedRoute>
              <DocumentsPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/documents" replace />} />
      </Routes>
      <Toast />
    </BrowserRouter>
  )
}