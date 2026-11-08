import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import DocumentsPage from './pages/DocumentsPage.jsx'
import Toast from './components/Toast.jsx'

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