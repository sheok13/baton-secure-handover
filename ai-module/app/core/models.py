"""LLM·임베딩 모델 생성 (D 소유, E의 chain.py도 사용).

어떤 공급자·모델을 쓸지는 코드가 아니라 환경변수(.env)가 정한다.
개발: Groq + Ollama bge-m3 / 배포: OpenAI (명세서 3.3)
"""
from functools import lru_cache

from langchain_core.embeddings import Embeddings
from langchain_core.language_models.chat_models import BaseChatModel

from app.core.config import get_settings