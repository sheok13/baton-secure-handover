import { request } from './apiClient.js'
import { documents } from '../mocks/data.js'
import { canRead } from '../mocks/canRead.js'
import { useAuthStore } from '../stores/authStore.js'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function listDocuments({ keyword = '', page = 0, size = 10 } = {}) {
  if (USE_MOCK) {
    const me = useAuthStore.getState().member
    const filtered = documents
      .filter((doc) => canRead(doc, me))                     // 권한 있는 문서만
      .filter((doc) => doc.title.includes(keyword))           // 제목에 검색어 포함 (빈 문자열이면 모두 통과)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))      // 최신순
    return {
      content: filtered.slice(page * size, page * size + size),
      page,
      totalPages: Math.ceil(filtered.length / size),
      totalElements: filtered.length,
    }
  }
  const res = await request(
    `/documents?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}&sort=createdAt,desc`,
  )
  return res.data
}

export async function downloadDocument(id) {
  if (USE_MOCK) {
    const me = useAuthStore.getState().member
    const doc = documents.find((d) => d.id === id)
    if (!doc || !canRead(doc, me)) {
      throw { status: 404, code: 'DOC_NOT_FOUND' }
    }
    return { message: '다운로드되었습니다.' }
  }
  return request(`/documents/${id}/download`)
}

export async function uploadDocument({ file, title, department, minRank }) {
  if (USE_MOCK) {
    await new Promise((resolve) => setTimeout(resolve, 800))
    const me = useAuthStore.getState().member
    const newDoc = {
      id: Date.now(),
      title: title || file.name.replace(/\.[^.]+$/, ''),
      createdAt: new Date().toISOString(),
      author: { name: me.name, rank: me.rank },
      department,
      minRank,
      status: 'READY',
    }
    documents.unshift(newDoc)
    return { id: newDoc.id, status: newDoc.status, chunkCount: 0, maskedCount: 0 }
  }
  // TODO: 실 API(multipart/form-data). apiClient가 FormData를 보낼 수 있게 고친 뒤 연결
  throw { status: 501, code: 'NOT_IMPLEMENTED' }
}