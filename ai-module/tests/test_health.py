import os

os.environ.setdefault("AI_INTERNAL_API_KEY", "test-internal-key-0123456789")

from fastapi import Depends, FastAPI  # noqa: E402
from fastapi.testclient import TestClient  # noqa: E402

from app.api.deps import verify_internal_key  # noqa: E402
from app.api.main import app  # noqa: E402

client = TestClient(app)


def test_health_without_key():
    res = client.get("/health")
    assert res.status_code == 200
    assert res.json() == {"status": "ok"}


def _guarded_client() -> TestClient:
    guarded = FastAPI()

    @guarded.get("/internal/ping", dependencies=[Depends(verify_internal_key)])
    def ping():
        return {"ok": True}

    return TestClient(guarded)


def test_internal_rejects_missing_or_wrong_key():
    c = _guarded_client()
    assert c.get("/internal/ping").status_code == 401
    assert c.get("/internal/ping", headers={"X-Internal-Api-Key": "wrong"}).status_code == 401


def test_internal_accepts_right_key():
    c = _guarded_client()
    res = c.get("/internal/ping", headers={"X-Internal-Api-Key": os.environ["AI_INTERNAL_API_KEY"]})
    assert res.status_code == 200
