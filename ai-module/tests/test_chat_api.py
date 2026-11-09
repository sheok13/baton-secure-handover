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


def events(res) -> list[tuple[str, object]]:
    """SSE 응답 본문을 [(이벤트 이름, data), ...] 로 바꾼다."""
    out = []
    for block in res.text.strip().split("\n\n"):
        lines = dict(line.split(": ", 1) for line in block.splitlines())
        out.append((lines["event"], json.loads(lines["data"])))
    return out

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

# ── 스트림이 시작되기 전 (진짜 HTTP 오류) ──
 
def test_rejects_missing_key():
    res = ask(headers={})
    assert res.status_code == 401
    assert res.json()["error"]["code"] == "INTERNAL_AUTH_FAILED"
 
 
def test_rejects_missing_filter():
    res = ask(flt=None)
    assert res.status_code == 400
    assert res.json()["error"]["code"] == "FILTER_INVALID"
 
 
def test_rejects_empty_filter():
    res = ask(flt={"departments": [], "ranks": ["STAFF"]})
    assert res.status_code == 400
    assert res.json()["error"]["code"] == "FILTER_INVALID"
 
 
def test_rejects_blank_question():
    res = ask(question="   ")
    assert res.status_code == 422
    assert res.json()["error"]["code"] == "VALIDATION_FAILED"

# ── 스트림 정상 흐름 ──
 
def test_normal_flow_token_sources_done(fake_e):
    res = ask()
    assert res.status_code == 200
    assert res.headers["content-type"].startswith("text/event-stream")
    assert res.headers["cache-control"] == "no-cache"
    assert res.headers["x-accel-buffering"] == "no"
    assert events(res) == [
        ("token", {"text": "주의할 점은 "}),
        ("token", {"text": "신원 확인입니다."}),
        ("sources", [{"docId": 3, "title": "거래처 관리 매뉴얼", "page": None}]),
        ("done", {}),
    ]
 
 
def test_korean_is_not_escaped(fake_e):
    # ensure_ascii=False: 한글이 \uXXXX 로 바뀌지 않아야 한다
    assert "주의할 점은" in ask().text

 # ── LLM을 부르지 않는 고정 응답 ──
 
def test_blocked_question_sends_fixed_message(fake_e):
    fake_e.setattr(routes_chat, "check_question", lambda q: GuardResult(blocked=True, reason="TEST"))
    fake_e.setattr(routes_chat, "retrieve", lambda q, f: pytest.fail("막힌 질문은 검색하면 안 된다"))
    assert events(ask()) == [("token", {"text": BLOCKED_MESSAGE}), ("sources", []), ("done", {})]
 
 
def test_no_hits_sends_fixed_message_without_llm(fake_e):
    async def must_not_call(*args):
        pytest.fail("0건이면 LLM을 부르면 안 된다")
        yield ""
 
    fake_e.setattr(routes_chat, "retrieve", lambda q, f: [])
    fake_e.setattr(routes_chat, "stream_answer", must_not_call)
    assert events(ask()) == [("token", {"text": NO_ANSWER_MESSAGE}), ("sources", []), ("done", {})]
 
 
def test_llm_no_answer_has_empty_sources(fake_e):
    async def no_answer(question, docs, history):
        yield NO_ANSWER_MESSAGE
 
    fake_e.setattr(routes_chat, "stream_answer", no_answer)
    assert ("sources", []) in events(ask())

    # ── 스트림 도중 문제 ──
 
def test_output_check_blocks_with_error(fake_e):
    fake_e.setattr(routes_chat, "check_output", lambda text: GuardResult(blocked="신원" in text))
    evs = events(ask())
    assert evs[0] == ("token", {"text": "주의할 점은 "})  # 걸리기 전 조각은 나간다
    assert evs[-1] == ("error", routes_chat.UNAVAILABLE)  # 걸린 조각은 안 나가고 error로 끝
    assert all(name != "sources" for name, _ in evs)
 
 
def test_exception_becomes_error_event(fake_e):
    def boom(q, f):
        raise RuntimeError("Ollama 꺼짐")
 
    fake_e.setattr(routes_chat, "retrieve", boom)
    res = ask()
    assert res.status_code == 200  # 스트림은 이미 시작됐으므로 200
    assert events(res) == [("error", {"code": "AI_UNAVAILABLE", "message": "잠시 후 다시 시도해 주세요."})]