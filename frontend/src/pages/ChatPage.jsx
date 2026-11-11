import { useEffect, useRef, useState } from 'react'
import ChatSidebar from '../components/ChatSidebar.jsx'
import ChatMessage from '../components/ChatMessage.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import { listSessions, listMessages, createSession, streamAnswer } from '../services/chatApi.js'

const ERROR_MESSAGES = {
  SESSION_NOT_FOUND: '대화를 찾을 수 없습니다. 새 대화로 다시 시도해 주세요.',
  NETWORK_ERROR: '네트워크 연결을 확인한 뒤 다시 시도해 주세요.',
  AI_UNAVAILABLE: '잠시 후 다시 시도해 주세요.',
  STREAM_INTERRUPTED: '답변이 중간에 끊겼습니다. 다시 시도해 주세요.',
}

export default function ChatPage() {
  const [sessions, setSessions] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(false)
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const lastQuestion = useRef('')
  const selection = useRef(0)
  const sending = useRef(false)
  const [pageError, setPageError] = useState('')

  useEffect(() => {
    let active = true
    listSessions().then((items) => { if (active) setSessions(items) })
      .catch(() => { if (active) setPageError('대화 목록을 불러오지 못했습니다. 다시 로그인하거나 새로고침해 주세요.') })
    return () => { active = false; selection.current += 1 }
  }, [])

  function handleSelect(id) {
    if (sending.current) return
    const version = ++selection.current
    setMessages([])
    setPageError('')
    setSelectedId(id)
    setLoading(true)
    listMessages(id)
      .then((items) => { if (version === selection.current) setMessages(items) })
      .catch(() => { if (version === selection.current) setPageError('대화를 불러오지 못했습니다. 다시 선택해 주세요.') })
      .finally(() => { if (version === selection.current) setLoading(false) })
  }

  function handleNew() {
    if (sending.current) return
    selection.current += 1
    setLoading(false)
    setPageError('')
    setInput('')
    setSelectedId(null)
    setMessages([])
  }

  const patch = (id, fn) =>
    setMessages((prev) => prev.map((m) => (m.id === id ? fn(m) : m)))

  async function sendQuestion(question) {
    if (sending.current || loading) return
    sending.current = true
    lastQuestion.current = question
    setStreaming(true)

    const assistantId = Date.now() + 1
    let sessionId = selectedId

    setMessages((prev) => [
      ...prev,
      { id: assistantId - 1, role: 'USER', content: question },
      { id: assistantId, role: 'ASSISTANT', content: '', streaming: true },
    ])

    const fail = (err) =>
      patch(assistantId, (m) => ({
        ...m,
        streaming: false,
        error: ERROR_MESSAGES[err?.code] ?? '잠시 후 다시 시도해 주세요.',
      }))

    try {
      if (sessionId === null) {
        const created = await createSession()
        sessionId = created.id
        setSelectedId(sessionId)
      }
      await streamAnswer(sessionId, question, {
        onToken: (text) => patch(assistantId, (m) => ({ ...m, content: m.content + text })),
        onSources: (sources) => patch(assistantId, (m) => ({ ...m, sources })),
        onDone: () => patch(assistantId, (m) => ({ ...m, streaming: false })),
        onError: fail,
      })
    } catch (err) {
      fail(err)
    } finally {
      sending.current = false
      setStreaming(false)
      listSessions().then(setSessions).catch(() => {})
    }
  }

  function handleSend(e) {
    e.preventDefault()
    const question = input.trim()
    if (!question || sending.current || loading) return
    setInput('')
    sendQuestion(question)
  }

  function handleRetry() {
    if (streaming) return
    setMessages((prev) => prev.slice(0, -2))
    sendQuestion(lastQuestion.current)
  }

  return (
    <div className="flex min-h-[70vh]">
      <ChatSidebar
        sessions={sessions}
        selectedId={selectedId}
        onSelect={handleSelect}
        onNew={handleNew}
      />
      <div className="flex flex-1 flex-col">
        <section className="flex-1 space-y-3 overflow-y-auto p-4">
          {selectedId === null && messages.length === 0 && (
            <p className="text-gray-500">질문을 입력하면 새 대화가 시작됩니다.</p>
          )}
          {pageError && <p role="alert" className="text-red-400">{pageError}</p>}
          {loading && <LoadingSpinner />}
          {messages.map((m, i) => (
            <ChatMessage
              key={m.id}
              message={m}
              onRetry={m.error && i === messages.length - 1 && !streaming ? handleRetry : undefined}
            />
          ))}
        </section>
        <form onSubmit={handleSend} className="flex gap-2 border-t border-gray-700 p-3">
          <input
            className="flex-1 rounded border border-gray-600 bg-transparent p-2"
            placeholder="질문을 입력하세요"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={streaming || loading}
            maxLength={1000}
          />
          <button className="rounded border px-4 py-2" disabled={streaming || loading}>
            보내기
          </button>
        </form>
      </div>
    </div>
  )
}