import { useEffect, useState } from 'react'
import ChatSidebar from '../components/ChatSidebar.jsx'
import ChatMessage from '../components/ChatMessage.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import { listSessions, listMessages, streamAnswer } from '../services/chatApi.js'

export default function ChatPage() {
  const [sessions, setSessions] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(false)
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)

  useEffect(() => {
    listSessions().then(setSessions)
  }, [])

  function handleSelect(id) {
    if (streaming) return // 답변이 오는 중에는 대화 전환 막기
    setSelectedId(id)
    setLoading(true)
    listMessages(id)
      .then(setMessages)
      .finally(() => setLoading(false))
  }

  async function handleSend(e) {
    e.preventDefault()
    const question = input.trim()
    if (!question || streaming || selectedId === null) return

    setInput('')
    setStreaming(true)

    const now = Date.now()
    setMessages((prev) => [
      ...prev,
      { id: now, role: 'USER', content: question },
      { id: now + 1, role: 'ASSISTANT', content: '' },
    ])

    try {
      await streamAnswer(question, (token) => {
        setMessages((prev) =>
          prev.map((m) => (m.id === now + 1 ? { ...m, content: m.content + token } : m)),
        )
      })
    } finally {
      setStreaming(false)
    }
  }

  return (
    <div className="flex min-h-[70vh]">
      <ChatSidebar sessions={sessions} selectedId={selectedId} onSelect={handleSelect} />
      <div className="flex flex-1 flex-col">
        <section className="flex-1 space-y-3 overflow-y-auto p-4">
          {selectedId === null && (
            <p className="text-gray-500">왼쪽에서 대화를 선택하세요.</p>
          )}
          {loading && <LoadingSpinner />}
          {messages.map((m) => (
            <ChatMessage key={m.id} message={m} />
          ))}
        </section>
        <form onSubmit={handleSend} className="flex gap-2 border-t border-gray-700 p-3">
          <input
            className="flex-1 rounded border border-gray-600 bg-transparent p-2"
            placeholder="질문을 입력하세요"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={streaming || selectedId === null}
          />
          <button className="rounded border px-4 py-2" disabled={streaming || selectedId === null}>
            보내기
          </button>
        </form>
      </div>
    </div>
  )
}