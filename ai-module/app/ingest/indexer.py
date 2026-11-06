"""청크에 권한 메타데이터를 붙여 Chroma에 저장·삭제 (D 소유).

권한 값(department, min_rank)은 Backend가 DB 기준으로 보낸 값을 그대로 쓴다.
문서 내용을 보고 추측하거나 바꾸지 않는다.
"""
from langchain_core.documents import Document

from app.core.constants import (
    DEPARTMENTS,
    META_CHUNK_INDEX,
    META_DEPT,
    META_DOC_ID,
    META_MIN_RANK,
    META_TITLE,
    RANKS,
)
from app.core.vectorstore import get_vectorstore

def index_chunks(
    *,
    doc_id: int,
    title: str,
    department: str,
    min_rank: str,
    chunks: list[Document],
) -> int:
    """청크를 저장하고 저장한 개수를 돌려준다."""
    # 마지막 방어선: 허용되지 않은 권한 값은 저장하지 않는다
    if department not in DEPARTMENTS or min_rank not in RANKS:
        raise ValueError("허용되지 않은 권한 값")

    tagged: list[Document] = []
    ids: list[str] = []
    for i, chunk in enumerate(chunks):
        metadata = {
            **chunk.metadata,            # loader가 붙인 page (PDF만)
            META_DOC_ID: doc_id,
            META_TITLE: title,
            META_DEPT: department,
            META_MIN_RANK: min_rank,
            META_CHUNK_INDEX: i,
        }
        tagged.append(Document(page_content=chunk.page_content, metadata=metadata))
        ids.append(f"{doc_id}-{i}")

    # 같은 문서를 다시 올리면 예전 청크를 먼저 지운다 (재시도 안전)
    delete_document(doc_id)
    try:
        get_vectorstore().add_documents(tagged, ids=ids)
    except Exception:
        delete_document(doc_id)  # 일부만 저장된 채로 남지 않게 정리
        raise
    return len(tagged)

