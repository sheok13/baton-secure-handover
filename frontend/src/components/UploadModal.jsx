import { useState } from 'react'
import { uploadDocument } from '../services/documentApi.js'
import { useAuthStore } from '../stores/authStore.js'
import { DEPT, DEPT_LABEL, RANK } from '../constants/index.js'

const MAX_SIZE = 10 * 1024 * 1024
const ALLOWED = ['.pdf', '.docx']

const ERROR_MESSAGES = {
  FILE_INVALID_TYPE: 'PDF 또는 DOCX 파일만 올릴 수 있습니다.',
  FILE_TOO_LARGE: '파일은 10MB 이하만 올릴 수 있습니다.',
  DOC_FORBIDDEN_SCOPE: '선택한 공개 범위로는 올릴 수 없습니다.',
  AI_INGEST_FAILED: '문서 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.',
}

export default function UploadModal({ onClose, onUploaded }) {
  const member = useAuthStore((s) => s.member)
  const [file, setFile] = useState(null)
  const [title, setTitle] = useState('')
  const [department, setDepartment] = useState(member?.department || DEPT.ALL)
  const [minRank, setMinRank] = useState(RANK.STAFF)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function handleFileChange(e) {
    const picked = e.target.files[0]
    if (!picked) return
    const okType = ALLOWED.some((ext) => picked.name.toLowerCase().endsWith(ext))
    if (!okType) {
      setFile(null)
      setError(ERROR_MESSAGES.FILE_INVALID_TYPE)
      return
    }
    if (picked.size > MAX_SIZE) {
      setFile(null)
      setError(ERROR_MESSAGES.FILE_TOO_LARGE)
      return
    }
    setError('')
    setFile(picked)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!file) {
      setError('파일을 선택해 주세요.')
      return
    }
    setLoading(true)
    setError('')
    try {
      await uploadDocument({ file, title, department, minRank })
      onUploaded()
      onClose()
    } catch (err) {
      setError(ERROR_MESSAGES[err.code] ?? '업로드에 실패했습니다.')
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black/60">
      <form
        onSubmit={handleSubmit}
        className="flex w-full max-w-md flex-col gap-3 rounded border bg-gray-800 p-6"
      >
        <h2 className="text-lg font-bold">문서 올리기</h2>
        <input type="file" accept=".pdf,.docx" onChange={handleFileChange} />
        <input
          className="rounded border p-2"
          maxLength={200}
          placeholder="문서명"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <select
          className="rounded border bg-gray-800 p-2"
          value={department}
          onChange={(e) => setDepartment(e.target.value)}
        >
          <option value={member?.department}>{DEPT_LABEL[member?.department]}</option>
          <option value={DEPT.ALL}>{DEPT_LABEL[DEPT.ALL]}</option>
        </select>
        <select
          className="rounded border bg-gray-800 p-2"
          value={minRank}
          onChange={(e) => setMinRank(e.target.value)}
        >
          <option value={RANK.STAFF}>전체</option>
          <option value={RANK.LEADER} disabled={member?.rank === RANK.STAFF}>
            팀장만
          </option>
        </select>
        {error && <p className="text-sm text-red-400">{error}</p>}
        <div className="flex justify-end gap-2">
          <button type="button" className="rounded border px-3 py-1" onClick={onClose}>
            취소
          </button>
          <button className="rounded border px-3 py-1" disabled={loading}>
            {loading ? '처리 중...' : '올리기'}
          </button>
        </div>
      </form>
    </div>
  )
}