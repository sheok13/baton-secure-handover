"""indexer 동작 확인: sample.docx를 적재 → 권한별 검색 → 삭제."""
from pathlib import Path

from app.core.vectorstore import get_vectorstore
from app.ingest.indexer import delete_document, index_chunks
from app.ingest.loader import load_document
from app.ingest.splitter import split_documents

p = Path("sample.docx")  # tests/fixtures로 옮겼다면 경로 수정
chunks = split_documents(load_document(p.read_bytes(), p.name))

count = index_chunks(doc_id=1, title="거래처 관리 매뉴얼", department="SALES", min_rank="STAFF", chunks=chunks)
print("저장한 조각 수:", count)

store = get_vectorstore()
question = "정산 마감일이 언제야?"

def search(label, depts, ranks):
    where = {"$and": [{"dept": {"$in": depts}}, {"min_rank": {"$in": ranks}}]}
    hits = store.similarity_search_with_relevance_scores(question, k=2, filter=where)
    print(f"\n[{label}] {len(hits)}건")
    for doc, score in hits:
        m = doc.metadata
        print(f"  {score:.3f}  doc_id={m['doc_id']} chunk={m['chunk_index']} {m['dept']}/{m['min_rank']}  {doc.page_content[:30]!r}")

search("영업 사원", ["SALES", "ALL"], ["STAFF"])
search("영업 팀장", ["SALES", "ALL"], ["STAFF", "LEADER"])
search("개발 사원", ["DEV", "ALL"], ["STAFF"])

print("\n다시 적재(재시도) 후 조각 수:", index_chunks(doc_id=1, title="거래처 관리 매뉴얼", department="SALES", min_rank="STAFF", chunks=chunks))
print("삭제한 조각 수:", delete_document(1))
print("한 번 더 삭제:", delete_document(1))