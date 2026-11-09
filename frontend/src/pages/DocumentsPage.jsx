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
    <div className="mx-auto max-w-4xl p-6">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-bold">문서함</h1>
        <button
          className="rounded border px-3 py-1"
          onClick={() => setUploadOpen(true)}
        >
          문서 올리기
        </button>
      </div>

      <form onSubmit={handleSearch} className="mb-4 flex gap-2">
        <input
          className="flex-1 rounded border p-2"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="문서명 검색"
        />
        <button className="rounded border px-4">검색</button>
      </form>

      {loading ? (
        <p className="py-8 text-center text-gray-500">불러오는 중...</p>
      ) : (
        <DocumentTable documents={data.content} onDownload={handleDownload} />
      )}

      <div className="mt-4 flex justify-center gap-2">
        <button
          className="rounded border px-3 py-1 disabled:opacity-40"
          disabled={page === 0}
          onClick={() => setPage(page - 1)}
        >
          이전
        </button>
        <button
          className="rounded border px-3 py-1 disabled:opacity-40"
          disabled={page + 1 >= data.totalPages}
          onClick={() => setPage(page + 1)}
        >
          다음
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