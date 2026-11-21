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
  VALIDATION_FAILED: '입력값을 확인해 주세요.',
  AI_UNAVAILABLE: '문서 처리를 위한 AI 서버가 준비되지 않았습니다. 잠시 후 다시 시도해 주세요.',
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
      const message =
        ERROR_MESSAGES[err.code] ??
        (err.status === 413 ? ERROR_MESSAGES.FILE_TOO_LARGE : '업로드에 실패했습니다.')
      setError(message)
      setLoading(false)
    }
  }

  const isStaff = member?.rank === RANK.STAFF

  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-ink/45 p-6">
      <form
        onSubmit={handleSubmit}
        className="flex w-full max-w-[560px] flex-col gap-5 rounded-xl bg-white p-7"
      >
        <h2 className="text-[22px] font-bold">문서 올리기</h2>

        <div className="flex flex-col gap-2">
          <span className="text-sm font-medium">파일</span>
          <div className="flex min-h-14 items-center justify-between gap-3 rounded-lg border border-field bg-white py-1.5 pl-3.5 pr-2">
            <span className={`min-w-0 truncate text-sm ${file ? 'text-ink' : 'text-[#6B7488]'}`}>
              {file ? file.name : '선택된 파일 없음'}
            </span>
            <label className="inline-flex min-h-11 shrink-0 cursor-pointer items-center rounded-lg border border-brand px-4 text-sm font-medium text-brand hover:bg-brand-soft">
              파일 선택
              <input
                type="file"
                accept=".pdf,.docx"
                onChange={handleFileChange}
                className="sr-only"
              />
            </label>
          </div>
          <span className="text-[13px] text-sub">PDF, DOCX · 최대 10MB</span>
        </div>

        <div className="flex flex-col gap-2">
          <label htmlFor="title" className="text-sm font-medium">
            문서명
          </label>
          <input
            id="title"
            className="h-12 rounded-lg border border-field px-3.5 text-[15px]"
            maxLength={200}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <label htmlFor="dept" className="text-sm font-medium">
            부서
          </label>
          <select
            id="dept"
            className="h-12 rounded-lg border border-field bg-white px-3.5 text-[15px]"
            value={department}
            onChange={(e) => setDepartment(e.target.value)}
          >
            <option value={member?.department}>{DEPT_LABEL[member?.department]}</option>
            <option value={DEPT.ALL}>{DEPT_LABEL[DEPT.ALL]}</option>
          </select>
        </div>

        <fieldset className="m-0 flex flex-col gap-2 border-0 p-0">
          <legend className="mb-2 p-0 text-sm font-medium">공개 직급</legend>
          <div className="flex gap-2">
            <label
              className={`flex min-h-11 flex-1 cursor-pointer items-center justify-center gap-1.5 rounded-lg text-sm ${
                minRank === RANK.STAFF
                  ? 'border-2 border-brand bg-brand-soft font-medium'
                  : 'border border-line text-[#6B7488]'
              }`}
            >
              <input
                type="radio"
                name="rank"
                checked={minRank === RANK.STAFF}
                onChange={() => setMinRank(RANK.STAFF)}
              />{' '}
              전체
            </label>
            <label
              className={`flex min-h-11 flex-1 items-center justify-center gap-1.5 rounded-lg text-sm ${
                isStaff ? 'border border-line text-[#6B7488]' : 'cursor-pointer'
              } ${
                !isStaff && minRank === RANK.LEADER
                  ? 'border-2 border-brand bg-brand-soft font-medium'
                  : !isStaff
                    ? 'border border-line'
                    : ''
              }`}
            >
              <input
                type="radio"
                name="rank"
                disabled={isStaff}
                checked={minRank === RANK.LEADER}
                onChange={() => setMinRank(RANK.LEADER)}
              />{' '}
              팀장만
            </label>
          </div>
          {isStaff && (
            <span className="text-[13px] text-sub">사원 계정은 "전체"로만 올릴 수 있어요.</span>
          )}
        </fieldset>

        {error && <p className="text-sm text-danger">{error}</p>}

        <div className="flex flex-wrap justify-end gap-3">
          <button
            type="button"
            className="inline-flex min-h-12 items-center rounded-lg border border-field bg-white px-5.5 text-[15px] font-medium hover:bg-bg"
            onClick={onClose}
          >
            취소
          </button>
          <button
            className="inline-flex min-h-12 items-center rounded-lg bg-brand px-5.5 text-[15px] font-medium text-white hover:bg-brand-dark disabled:opacity-60"
            disabled={loading}
          >
            {loading ? '처리 중...' : '올리기'}
          </button>
        </div>
      </form>
    </div>
  )
}
