import { DEPT_LABEL, RANK_LABEL } from '../constants/index.js'

const GRID =
  'grid min-w-[860px] grid-cols-[minmax(0,2.4fr)_minmax(0,1.2fr)_minmax(0,1fr)_minmax(0,0.8fr)_minmax(0,1fr)_minmax(0,0.8fr)] items-center gap-3 px-5'

export default function DocumentTable({ documents, onDownload }) {
  if (documents.length === 0) {
    return (
      <div className="rounded-xl border border-line bg-white py-12 text-center text-sub">
        문서가 없습니다.
      </div>
    )
  }
  return (
    <div className="overflow-x-auto rounded-xl border border-line bg-white">
      <div className={`${GRID} rounded-t-xl border-b border-line bg-[#F8F9FC] py-3 text-[13px] font-medium text-sub`}>
        <span>문서명</span>
        <span>작성일</span>
        <span>작성자</span>
        <span>직급</span>
        <span>부서</span>
        <span>관리</span>
      </div>
      {documents.map((doc) => (
        <div key={doc.id} className={`${GRID} min-h-16 border-b border-[#ECEFF5] py-2 last:border-b-0`}>
          <span className="min-w-0 text-[15px] font-medium">{doc.title}</span>
          <span className="text-sm">{doc.createdAt.slice(0, 10)}</span>
          <span className="text-sm font-medium">{doc.author.name}</span>
          <span className="text-sm text-[#3A4458]">{RANK_LABEL[doc.author.rank]}</span>
          <span className="text-sm text-[#3A4458]">{DEPT_LABEL[doc.department]}</span>
          <span className="flex gap-2">
            <button
              type="button"
              aria-label={`${doc.title} 다운로드`}
              className="inline-flex h-11 w-11 items-center justify-center rounded-lg border border-field bg-white hover:bg-bg"
              onClick={() => onDownload(doc.id)}
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M12 4v11M7 11l5 5 5-5" />
                <path d="M5 20h14" />
              </svg>
            </button>
          </span>
        </div>
      ))}
    </div>
  )
}
