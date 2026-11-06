"""FastAPI 진입점 (D 소유).

실행: uv run uvicorn app.api.main:app --reload --port 8000
"""
from fastapi import APIRouter, Depends, FastAPI

from app.api.deps import verify_internal_key
from app.api.errors import register_error_handlers
from app.api.routes_ingest import router as ingest_router
from app.core.config import get_settings

get_settings()  # 필수 환경변수 누락 시 여기서 기동 실패

app = FastAPI(title="Baton AI Module", docs_url=None, redoc_url=None, openapi_url=None)
register_error_handlers(app)


@app.get("/health")
def health() -> dict:
    # TODO(vectorstore): 벡터 스토어를 열 수 없으면 503 반환 (명세서 4.4)
    return {"status": "ok"}


# /internal/* 는 전부 내부 API Key 필수
internal = APIRouter(prefix="/internal", dependencies=[Depends(verify_internal_key)])
internal.include_router(ingest_router)
app.include_router(internal)