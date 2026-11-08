import { chatSessions, chatMessages } from '../mocks/chat';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export async function listSessions() {
  if (USE_MOCK) {
    await delay(300);
    return [...chatSessions].sort((a, b) => b.updatedAt.localeCompare(a.updatedAt));
  }
  throw new Error('TODO: 실 API');
}

export async function listMessages(sessionId) {
  if (USE_MOCK) {
    await delay(300);
    return chatMessages[sessionId] ?? [];
  }
  throw new Error('TODO: 실 API');
}

function mockStream(text) {
  const encoder = new TextEncoder();
  const words = text.split(' ');
  return new ReadableStream({
    async start(controller) {
      for (const w of words) {
        await delay(60);
        controller.enqueue(encoder.encode(w + ' '));
      }
      controller.close();
    },
  });
}

export async function streamAnswer(question, onToken) {
  if (!USE_MOCK) throw new Error('TODO: 실 API');
  const reader = mockStream('가짜 답변입니다 출처 문서를 확인해 보세요').getReader();
  const decoder = new TextDecoder();
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    onToken(decoder.decode(value, { stream: true }));
  }
}