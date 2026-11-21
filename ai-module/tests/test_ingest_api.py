"""문서 수집·삭제 API 테스트 (D 소유).
 
임베딩·Chroma 저장(index_chunks, delete_document)은 가짜로 바꿔 끼운다.
그래서 Ollama 없이 '받기 → 꺼내기 → 마스킹 → 자르기 → 저장 호출 → 응답'만 검사한다.
"""
import os
from types import SimpleNamespace
 
# 테스트용 필수 환경변수 (실제 키 아님). app import 전에 설정해야 한다.
os.environ.setdefault("AI_INTERNAL_API_KEY", "test-internal-key-0123456789")
os.environ.setdefault("LLM_PROVIDER", "groq")
os.environ.setdefault("LLM_MODEL", "test-model")
os.environ.setdefault("GROQ_API_KEY", "test-groq-key")
os.environ.setdefault("VECTOR_STORE_PATH", "./data/chroma/test")
 
import pytest  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402
 
from app.api import routes_ingest  # noqa: E402
from app.api.main import app  # noqa: E402

client = TestClient(app)
HEADERS = {"X-Internal-Api-Key": os.environ["AI_INTERNAL_API_KEY"]}
FORM = {"docId": "4", "title": "월간 영업보고", "department": "SALES", "minRank": "STAFF"}

@pytest.fixture
def saved(monkeypatch):
    """index_chunks를 가짜로: 받은 값을 기록하고 조각 수를 돌려준다."""
    calls = []
 
    def fake_index(**kwargs):
        calls.append(kwargs)
        return len(kwargs["chunks"])
 
    monkeypatch.setattr(routes_ingest, "index_chunks", fake_index)
    return calls
 
 
def upload(data: bytes, filename: str, form=None, headers=HEADERS):
    files = {"file": (filename, data, "application/octet-stream")}
    return client.post("/internal/ingest", data=form or FORM, files=files, headers=headers)

# ── 정상 ──
 
def test_ingest_masks_before_saving(saved, make_docx):
    res = upload(make_docx(["비상 연락망 010-0000-1234", "법인 계좌 테스트은행 999-99-999999"]), "report.docx")
    assert res.status_code == 200
    assert res.json() == {"docId": 4, "chunkCount": 1, "maskedCount": 2}
 
    call = saved[0]
    assert (call["doc_id"], call["department"], call["min_rank"]) == (4, "SALES", "STAFF")
    stored = " ".join(c.page_content for c in call["chunks"])
    assert "[연락처]" in stored and "[계좌번호]" in stored
    assert "010-0000-1234" not in stored and "999-99-999999" not in stored  # 원문은 저장소로 안 간다
 
 
# ── 입력 오류 (Backend 검증 뒤 2차 방어선) ──
 
def test_rejects_missing_key(saved, make_docx):
    assert upload(make_docx(["x"]), "a.docx", headers={}).status_code == 401
    assert saved == []
 
 
def test_rejects_unsupported_type(saved):
    res = upload(b"hello", "note.txt")
    assert res.status_code == 400
    assert res.json()["error"]["code"] == "FILE_INVALID_TYPE"
 
 
def test_rejects_too_large(saved, monkeypatch):
    monkeypatch.setattr(routes_ingest, "get_settings", lambda: SimpleNamespace(max_upload_mb=1))
    res = upload(b"0" * (1024 * 1024 + 1), "big.pdf")
    assert res.status_code == 413
    assert res.json()["error"]["code"] == "FILE_TOO_LARGE"
    assert saved == []
 
 
def test_rejects_pdf_without_text(saved, make_pdf):
    res = upload(make_pdf([""]), "scan.pdf")
    assert res.status_code == 422
    assert res.json()["error"]["code"] == "INGEST_EMPTY_TEXT"
 
 
def test_rejects_wrong_department(saved, make_docx):
    res = upload(make_docx(["x"]), "a.docx", form={**FORM, "department": "영업"})
    assert res.status_code == 422
    assert res.json()["error"]["code"] == "VALIDATION_FAILED"
 
 
def test_rejects_empty_title(saved, make_docx):
    # Backend가 제목을 비워 보내면 422 → B에게 '파일명으로 채워 달라' 요청한 이유
    res = upload(make_docx(["x"]), "a.docx", form={**FORM, "title": ""})
    assert res.status_code == 422

# ── 저장 실패 ──
 
def test_store_failure_becomes_502(monkeypatch, make_docx):
    def boom(**kwargs):
        raise RuntimeError("Ollama 꺼짐")
 
    monkeypatch.setattr(routes_ingest, "index_chunks", boom)
    res = upload(make_docx(["내용"]), "a.docx")
    assert res.status_code == 502
    assert res.json()["error"]["code"] == "UPSTREAM_FAILED"
    assert "Ollama" not in res.text  # 내부 사정은 응답에 넣지 않는다
 
 
