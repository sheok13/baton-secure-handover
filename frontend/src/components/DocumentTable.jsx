import { DEPT_LABEL, RANK_LABEL } from '../constants/index.js'

export default function DocumentTable({ documents, onDownload }) {
  if (documents.length === 0) {
    return <p className="py-8 text-center text-gray-500">문서가 없습니다.</p>
  }
  return (
    <table className="w-full border-collapse text-left">
      <thead>
        <tr className="border-b">
          <th className="p-2">문서명</th>
          <th className="p-2">작성일</th>
          <th className="p-2">작성자</th>
          <th className="p-2">직급</th>
          <th className="p-2">부서</th>
          <th className="p-2">관리</th>
        </tr>
      </thead>
      <tbody>
        {documents.map((doc) => (
          <tr key={doc.id} className="border-b">
            <td className="p-2">{doc.title}</td>
            <td className="p-2">{doc.createdAt.slice(0, 10)}</td>
            <td className="p-2">{doc.author.name}</td>
            <td className="p-2">{RANK_LABEL[doc.author.rank]}</td>
            <td className="p-2">{DEPT_LABEL[doc.department]}</td>
            <td className="p-2">
              <button
                className="rounded border px-2 py-1"
                onClick={() => onDownload(doc.id)}
              >
                다운로드
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}