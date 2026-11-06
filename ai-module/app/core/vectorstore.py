"""Chroma 벡터 저장소 열기 (D 소유, E의 retriever도 사용)."""
from functools import lru_cache

from langchain_chroma import Chroma

from app.core.config import get_settings
from app.core.models import get_embeddings