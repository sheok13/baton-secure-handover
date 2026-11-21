import { useEffect, useState } from 'react'
import { downloadDocument, listDocuments } from '../services/documentApi.js'
import { useToastStore } from '../stores/toastStore.js'
import DocumentTable from '../components/DocumentTable.jsx'
import UploadModal from '../components/UploadModal.jsx'

export default function DocumentsPage() {
  const [keyword, setKeyword] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [reloadKey, setReloadKey] = useState(0)
  const [uploadOpen, setUploadOpen] = useState(false)
  const show = useToastStore((s) => s.show)

  useEffect(() => {
    let ignore = false
    listDocuments({ keyword: query, page }).then((res) => {
      if (ignore) return
      setData(res)
      setLoading(false)
    })
    return () => {
      ignore = true
    }
  }, [query, page, reloadKey])

  function handleSearch(e) {
    e.preventDefault()
    setQuery(keyword)
    setPage(0)
  }

  async function handleDownload(id) {
    try {
      const res = await downloadDocument(id)
      show(res.message ?? '다운로드되었습니다.')
    } catch (err) {
      if (err.code === 'DOC_NOT_FOUND') {
        show('문서를 찾을 수 없습니다.')
        setReloadKey((k) => k + 1)
      }
    }
  }

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-[26px] font-bold">문서함</h1>
        <form onSubmit={handleSearch} className="flex flex-wrap items-center gap-3">
          <input
            type="search"
            aria-label="문서 검색"
            className="h-11 w-70 rounded-lg border border-field bg-white px-3.5 text-sm"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="제목으로 검색"
          />
          <button
            type="button"
            className="inline-flex min-h-11 items-center rounded-lg bg-brand px-5 text-[15px] font-medium text-white hover:bg-brand-dark"
            onClick={() => setUploadOpen(true)}
          >
            + 올리기
          </button>
        </form>
      </div>

      {loading ? (
        <p className="py-8 text-center text-sub">불러오는 중...</p>
      ) : (
        <DocumentTable documents={data.content} onDownload={handleDownload} />
      )}

      <div className="flex items-center justify-center gap-4 text-sm text-[#3A4458]">
        <button
          type="button"
          aria-label="이전 페이지"
          className="min-h-11 min-w-11 rounded-lg border border-field bg-white disabled:opacity-40"
          disabled={page === 0}
          onClick={() => setPage(page - 1)}
        >
          ‹
        </button>
        <span>
          {page + 1} / {Math.max(data.totalPages, 1)} 페이지
        </span>
        <button
          type="button"
          aria-label="다음 페이지"
          className="min-h-11 min-w-11 rounded-lg border border-field bg-white disabled:opacity-40"
          disabled={page + 1 >= data.totalPages}
          onClick={() => setPage(page + 1)}
        >
          ›
        </button>
      </div>

      {uploadOpen && (
        <UploadModal
          onClose={() => setUploadOpen(false)}
          onUploaded={() => setReloadKey((k) => k + 1)}
        />
      )}
    </div>
  )
}
