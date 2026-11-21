"""Chroma 벡터 저장소 열기 (D 소유, E의 retriever도 사용)."""
from functools import lru_cache

from langchain_chroma import Chroma

from app.core.config import get_settings
from app.core.models import get_embeddings

@lru_cache
def get_vectorstore() -> Chroma:
    s = get_settings()
    return Chroma(
        collection_name=s.chroma_collection,           # baton_docs
        embedding_function=get_embeddings(),           # 글자→숫자 변환에 쓸 모델
        persist_directory=s.vector_store_path,         # 디스크 저장 위치
        collection_metadata={"hnsw:space": "cosine"},  # 가까움을 재는 방법
    )