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