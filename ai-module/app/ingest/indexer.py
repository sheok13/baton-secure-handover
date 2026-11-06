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