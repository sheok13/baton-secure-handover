"""LLM·임베딩 모델 생성 (D 소유, E의 chain.py도 사용).

어떤 공급자·모델을 쓸지는 코드가 아니라 환경변수(.env)가 정한다.
개발: Groq + Ollama bge-m3 / 배포: OpenAI (명세서 3.3)
"""
from functools import lru_cache

from langchain_core.embeddings import Embeddings
from langchain_core.language_models.chat_models import BaseChatModel

from app.core.config import get_settings

@lru_cache
def get_llm() -> BaseChatModel:
    s = get_settings()

    if s.llm_provider == "groq":
        from langchain_groq import ChatGroq

        kwargs = {}
        if s.llm_model.startswith("openai/gpt-oss"):
            # gpt-oss는 추론 모델: 생각을 짧게 해서 첫 글자가 빨리 나오게
            kwargs["reasoning_effort"] = "low"

        return ChatGroq(
            model=s.llm_model,
            api_key=s.groq_api_key,
            streaming=True,
            timeout=s.llm_timeout_seconds,
            max_retries=1,
            **kwargs,
        )
    from langchain_openai import ChatOpenAI

    return ChatOpenAI(
        model=s.llm_model,
        api_key=s.openai_api_key,
        streaming=True,
        timeout=s.llm_timeout_seconds,
        max_retries=1,
    )