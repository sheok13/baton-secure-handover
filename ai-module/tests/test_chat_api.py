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

client = TestClient(app)
HEADERS = {"X-Internal-Api-Key": os.environ["AI_INTERNAL_API_KEY"]}
SALES_STAFF = {"departments": ["SALES", "ALL"], "ranks": ["STAFF"]}
DOC = Document(page_content="가짜 내용", metadata={"doc_id": 3, "title": "거래처 관리 매뉴얼"})

def ask(question="거래처 등록 주의점?", flt=SALES_STAFF, headers=HEADERS):
    body = {"question": question, "history": []}
    if flt is not None:
        body["filter"] = flt
    return client.post("/internal/chat", json=body, headers=headers)

@pytest.fixture
def fake_e(monkeypatch):
    """E 함수 기본값: 질문 통과, 문서 1건 찾음, 답 두 조각, 출력 통과."""
 
    async def answer(question, docs, history):
        for t in ["주의할 점은 ", "신원 확인입니다."]:
            yield t

    monkeypatch.setattr(routes_chat, "check_question", lambda q: GuardResult(blocked=False))
    monkeypatch.setattr(routes_chat, "retrieve", lambda q, f: [(DOC, 0.9)])
    monkeypatch.setattr(routes_chat, "stream_answer", answer)
    monkeypatch.setattr(routes_chat, "check_output", lambda text: GuardResult(blocked=False))
    return monkeypatch