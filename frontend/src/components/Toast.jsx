import { useEffect } from 'react'
import { useToastStore } from '../stores/toastStore.js'

export default function Toast() {
  const message = useToastStore((s) => s.message)
  const hide = useToastStore((s) => s.hide)

  useEffect(() => {
    if (!message) return
    const timer = setTimeout(hide, 3000)
    return () => clearTimeout(timer)
  }, [message, hide])

  if (!message) return null
  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 rounded bg-gray-800 px-4 py-2 text-white">
      {message}
    </div>
  )
}