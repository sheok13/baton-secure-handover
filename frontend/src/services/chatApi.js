import { request, refresh, BASE, toError, readJson } from './apiClient.js'
import { useAuthStore } from '../stores/authStore.js'
import { chatSessions, chatMessages } from '../mocks/chat'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
// mock 시간 문자열. mocks/chat.js의 updatedAt 형식과 다르면 이 한 줄만 맞추세요
const nowText = () => new Date().toISOString()

export async function listSessions() {
  let list
  if (USE_MOCK) {
    await delay(300)
    list = [...chatSessions]
  } else {
    const res = await request('/chat/sessions')
    list = [...res.data] // 정렬이 원본을 바꾸지 않게 복사
  }
  // 서버 정렬 보장 여부를 코드로 확인하지 못해 양쪽 모두 정렬
  return list.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
}

export async function createSession() {
  if (USE_MOCK) {
    await delay(200)
    const session = { id: Date.now(), title: '새 대화', updatedAt: nowText() }
    chatSessions.push(session) // 완료 후 목록을 다시 불러와도 사라지지 않게
    return { id: session.id, title: session.title, createdAt: session.updatedAt }
  }
  const res = await request('/chat/sessions', { method: 'POST' }) // 본문 없음, 201
  return res.data
}

export async function listMessages(sessionId) {
  if (USE_MOCK) {
    await delay(300)
    return chatMessages[sessionId] ?? []
  }
  const res = await request(`/chat/sessions/${sessionId}/messages`)
  return res.data
}

function mockStream(text) {
  const encoder = new TextEncoder()
  const words = text.split(' ')
  return new ReadableStream({
    async start(controller) {
      for (const w of words) {
        await delay(60)
        controller.enqueue(encoder.encode(w + ' '))
      }
      controller.close()
    },
  })
}

// 스트림 시작 요청. request()와 같은 규칙(토큰, 401 재시도 1회, 오류 변환)
async function openStream(sessionId, question, retry = false) {
  const headers = { 'Content-Type': 'application/json' }
  const token = useAuthStore.getState().accessToken
  if (token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(`${BASE}/chat/sessions/${sessionId}/messages`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ question }),
      credentials: 'include',
    })
  } catch {
    throw { status: 0, code: 'NETWORK_ERROR' }
  }

  if (res.status === 401 && !retry) {
    await refresh() // 실패하면 여기서 throw됨
    return openStream(sessionId, question, true)
  }
  // 400/404 등은 스트림이 시작되기 전에 JSON 오류로 온다
  if (!res.ok) throw toError(res.status, await readJson(res))
  return res
}

// 이벤트 한 덩어리("event: ...\ndata: ...")에서 이름과 데이터를 꺼낸다
function parseBlock(block) {
  let event = 'message'
  const dataLines = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).replace(/^ /, '')
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
  }
  return { event, data: dataLines.join('\n') }
}

async function readSse(res, handlers) {
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let finished = false // done 또는 error를 받았는지

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true }) // 글자가 청크 경계에서 끊겨도 안 깨지게
    buffer = buffer.replace(/\r\n/g, '\n')

    let idx
    while ((idx = buffer.indexOf('\n\n')) !== -1) {
      const block = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 2)
      if (!block.trim()) continue

      const { event, data } = parseBlock(block)

      let payload
      try {
        payload = JSON.parse(data)
      } catch {
        // 이벤트 하나가 깨졌다고 답변 전체를 끊지 않는다 (서버는 항상 JSON을 보냄)
        throw new Error('답변 데이터를 읽을 수 없습니다. 다시 시도해 주세요.')
      }

      if (event === 'done' || event === 'error') finished = true
      if (event === 'token') handlers.onToken?.(payload.text)
      else if (event === 'sources') handlers.onSources?.(payload)
      else if (event === 'done') handlers.onDone?.(payload)
      else if (event === 'error') handlers.onError?.(payload)
      if (finished) {
        await reader.cancel()
        return
      }
    }
  }

  // done/error 없이 연결이 끝남 → 서버가 오류를 보내지 못한 상황
  if (!finished) {
    handlers.onError?.({ code: 'STREAM_INTERRUPTED', message: '답변이 중간에 끊겼습니다.' })
  }
}

export async function streamAnswer(sessionId, question, handlers) {
  if (USE_MOCK) {
    const reader = mockStream('가짜 답변입니다 출처 문서를 확인해 보세요').getReader()
    const decoder = new TextDecoder()
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      handlers.onToken?.(decoder.decode(value, { stream: true }))
    }
    handlers.onSources?.([
      { docId: 1, title: '영업팀 인수인계서.pdf', page: 3 },
      { docId: 2, title: '개발팀 인수인계서.docx', page: null }, // DOCX는 page가 없음
    ])
    handlers.onDone?.({ messageId: Date.now() })
    return
  }
  const res = await openStream(sessionId, question)
  await readSse(res, handlers)
}