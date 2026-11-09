"""chat SSE 라우트 테스트 (D 소유).
 
E의 검색·답변·검사 함수는 가짜로 바꿔 끼운다(monkeypatch).
그래서 Ollama·Groq·Chroma 없이도 D가 만든 '받기 → 순서대로 호출 → SSE 보내기'만 검사할 수 있다.
"""

import json
import os

# 테스트용 필수 환경변수 (실제 키 아님). app import 전에 설정해야 한다.
TEST_KEY = "test-internal-key-0123456789"
os.environ.setdefault("AI_INTERNAL_API_KEY", TEST_KEY)
os.environ.setdefault("LLM_PROVIDER", "groq")
os.environ.setdefault("LLM_MODEL", "test-model")
os.environ.setdefault("GROQ_API_KEY", "test-groq-key")
os.environ.setdefault("VECTOR_STORE_PATH", "./data/chroma/test")

import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402
from langchain_core.documents import Document  # noqa: E402
 
from app.api import routes_chat  # noqa: E402
from app.api.main import app  # noqa: E402
from app.guard.types import GuardResult  # noqa: E402
from app.prompts.system_prompt import BLOCKED_MESSAGE, NO_ANSWER_MESSAGE  # noqa: E402