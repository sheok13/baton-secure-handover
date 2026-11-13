"""환경변수 → Settings (D 소유).
 
모델·공급자·경로·키는 코드에 직접 쓰지 않고 여기서만 읽는다.
필수 값이 비어 있으면 서버가 기동하지 않는다(fail-fast, 명세서 3.2).
"""
from functools import lru_cache
from typing import Literal
 
from pydantic import SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
 
 
class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        env_ignore_empty=True,  # "KEY=" 처럼 빈 값은 '없음'으로 취급 → 기본값 사용 또는 필수값 오류
    )
 
    # 공통
    llm_provider: Literal["groq", "openai"] = "groq"
    llm_model: str = ""
    llm_timeout_seconds: int = 60
    embedding_provider: Literal["ollama", "openai"] = "ollama"
    embedding_model: str = "bge-m3"
    vector_store_path: str = ""
    chroma_collection: str = "baton_docs"
    ai_internal_api_key: SecretStr  # 기본값 없음 → 없으면 기동 실패
 
    # RAG 파라미터
    chunk_size: int = 800
    chunk_overlap: int = 150
    retriever_k: int = 4
    relevance_threshold: float | None = None  # 임베딩 모델별 튜닝 후 기입
    max_upload_mb: int = 10
    max_question_chars: int = 1000
    max_history_messages: int = 6
 
    # 공급자 키 (선택한 공급자 것만 필요). SecretStr: print·로그에 '**********'로 가려짐
    groq_api_key: SecretStr = SecretStr("")
    ollama_base_url: str = "http://localhost:11434"
    openai_api_key: SecretStr = SecretStr("")
 
    @model_validator(mode="after")
    def _check_required(self) -> "Settings":
        missing: list[str] = []
 
        if len(self.ai_internal_api_key.get_secret_value()) < 16:
            missing.append("AI_INTERNAL_API_KEY(16자 이상)")
        if not self.llm_model:
            missing.append("LLM_MODEL")
        if not self.vector_store_path:
            missing.append("VECTOR_STORE_PATH")
 
        if self.llm_provider == "groq" and not self.groq_api_key.get_secret_value():
            missing.append("GROQ_API_KEY")
        if "openai" in (self.llm_provider, self.embedding_provider) and not self.openai_api_key.get_secret_value():
            missing.append("OPENAI_API_KEY")
        if self.embedding_provider == "ollama" and not self.ollama_base_url:
            missing.append("OLLAMA_BASE_URL")
 
        if missing:
            # 어떤 '이름'이 비었는지만 알린다. 값은 메시지에 넣지 않는다.
            raise ValueError("필수 환경변수가 비어 있습니다: " + ", ".join(missing))
        return self
 
 
@lru_cache
def get_settings() -> Settings:
    return Settings()