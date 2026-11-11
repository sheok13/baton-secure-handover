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
  const bottomRef = useRef(null)

  useEffect(() => {
    listSessions().then(setSessions)
  }, [])

  // 새 메시지·스트리밍 글자가 늘어날 때 맨 아래로 따라간다
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' })
  }, [messages])

  function handleSelect(id) {
    if (streaming) return // 답변이 오는 중에는 대화 전환 막기
    setSelectedId(id)
    setLoading(true)
    listMessages(id)
      .then(setMessages)
      .finally(() => setLoading(false))
  }

  function handleNew() {
    if (streaming) return
    setSelectedId(null)
    setMessages([])
  }

  const patch = (id, fn) =>
    setMessages((prev) => prev.map((m) => (m.id === id ? fn(m) : m)))

  async function sendQuestion(question) {
    if (streaming) return
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
      setStreaming(false)
      listSessions().then(setSessions).catch(() => {})
    }
  }

  function handleSend(e) {
    e.preventDefault()
    const question = input.trim()
    if (!question || streaming) return
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
          {loading && <LoadingSpinner />}
          {messages.map((m, i) => (
            <ChatMessage
              key={m.id}
              message={m}
              onRetry={m.error && i === messages.length - 1 && !streaming ? handleRetry : undefined}
            />
          ))}
          <div ref={bottomRef} />
        </section>
        <form onSubmit={handleSend} className="flex gap-2 border-t border-gray-700 p-3">
          <input
            className="flex-1 rounded border border-gray-600 bg-transparent p-2"
            placeholder="질문을 입력하세요"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={streaming}
            maxLength={1000}
          />
          <button className="rounded border px-4 py-2" disabled={streaming}>
            보내기
          </button>
        </form>
      </div>
    </div>
  )
}