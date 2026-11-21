import { Navigate } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore.js'

export default function ProtectedRoute({ children }) {
  const ready = useAuthStore((s) => s.ready)
  const accessToken = useAuthStore((s) => s.accessToken)

  if (!ready) return null
  if (!accessToken) return <Navigate to="/login" replace />
  return children
}