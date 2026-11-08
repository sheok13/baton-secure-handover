import os
 
# 테스트용 필수 환경변수 (실제 키 아님). app import 전에 설정해야 한다.
TEST_KEY = "test-internal-key-0123456789"
os.environ.setdefault("AI_INTERNAL_API_KEY", TEST_KEY)
os.environ.setdefault("LLM_PROVIDER", "groq")
os.environ.setdefault("LLM_MODEL", "test-model")
os.environ.setdefault("GROQ_API_KEY", "test-groq-key")
os.environ.setdefault("VECTOR_STORE_PATH", "./data/chroma/test")
 
from fastapi import Depends, FastAPI  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402
 
from app.api.deps import verify_internal_key  # noqa: E402
from app.api.errors import register_error_handlers  # noqa: E402
from app.api.main import app  # noqa: E402
 
client = TestClient(app)
 
 
def test_health_without_key():
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json() == {"status": "ok"}
 
 
def _guarded_client() -> TestClient:
    guarded = FastAPI()
    register_error_handlers(guarded)
 
    @guarded.get("/internal/ping", dependencies=[Depends(verify_internal_key)])
    def ping():
        return {"ok": True}
 
    return TestClient(guarded)
 
 
def test_internal_rejects_missing_or_wrong_key():
    c = _guarded_client()
    for headers in ({}, {"X-Internal-Api-Key": "wrong"}):
        res = c.get("/internal/ping", headers=headers)
        assert res.status_code == 401
        assert res.json()["error"]["code"] == "INTERNAL_AUTH_FAILED"  # 명세서 4.5 모양
 
 
def test_internal_rejects_non_ascii_key_with_401_not_500():
    c = _guarded_client()
    res = c.get("/internal/ping", headers={"X-Internal-Api-Key": "키값테스트".encode("utf-8")})
    assert res.status_code == 401
 
 
def test_internal_accepts_right_key():
    c = _guarded_client()
    res = c.get("/internal/ping", headers={"X-Internal-Api-Key": os.environ["AI_INTERNAL_API_KEY"]})
    assert res.status_code == 200
 
 
def test_unknown_path_uses_error_shape():
    res = client.get("/no-such-path")
    assert res.status_code == 404
    assert "error" in res.json()