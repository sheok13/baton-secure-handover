import { useEffect, useState } from 'react'
import ChatSidebar from '../components/ChatSidebar.jsx'
import ChatMessage from '../components/ChatMessage.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import { listSessions, listMessages } from '../services/chatApi.js'

export default function ChatPage() {
  const [sessions, setSessions] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(false)

  // 대화 목록은 처음 한 번만 가져온다
  useEffect(() => {
    listSessions().then(setSessions)
  }, [])

  // 대화를 고르면 그 대화의 메시지를 가져온다
  function handleSelect(id) {
    setSelectedId(id)
    setLoading(true)
    listMessages(id)
      .then(setMessages)
      .finally(() => setLoading(false))
  }

  return (
    <div className="flex min-h-[70vh]">
      <ChatSidebar sessions={sessions} selectedId={selectedId} onSelect={handleSelect} />
      <section className="flex-1 space-y-3 overflow-y-auto p-4">
        {selectedId === null && (
          <p className="text-gray-500">왼쪽에서 대화를 선택하세요.</p>
        )}
        {loading && <LoadingSpinner />}
        {messages.map((m) => (
          <ChatMessage key={m.id} message={m} />
        ))}
      </section>
    </div>
  )
}