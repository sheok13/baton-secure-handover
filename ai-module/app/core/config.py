"""환경변수 → Settings (D 소유).

모델·공급자·경로는 코드에 직접 쓰지 않고 여기서만 읽는다.
AI_INTERNAL_API_KEY가 비어 있으면 서버가 기동하지 않는다(fail-fast).
"""
from functools import lru_cache
from typing import Literal

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # 공통
    llm_provider: Literal["groq", "openai"] = "groq"
    llm_model: str = ""
    llm_timeout_seconds: int = 60
    embedding_provider: Literal["ollama", "openai"] = "ollama"
    embedding_model: str = "bge-m3"
    vector_store_path: str = "./data/chroma/bge-m3"
    chroma_collection: str = "baton_docs"
    ai_internal_api_key: str = Field(min_length=16)  # 필수

    # RAG 파라미터
    chunk_size: int = 800
    chunk_overlap: int = 150
    retriever_k: int = 4
    relevance_threshold: float | None = None  # 임베딩 모델별 튜닝 후 기입
    max_upload_mb: int = 10
    max_question_chars: int = 1000
    max_history_messages: int = 6

    # 공급자 키 (선택한 공급자 것만 필요)
    groq_api_key: str = ""
    ollama_base_url: str = "http://localhost:11434"
    openai_api_key: str = ""


@lru_cache
def get_settings() -> Settings:
    return Settings()
