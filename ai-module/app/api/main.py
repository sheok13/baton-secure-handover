"""FastAPI 진입점 (D 소유).

실행: uv run uvicorn app.api.main:app --reload --port 8000
"""
from fastapi import APIRouter, Depends, FastAPI

from app.api.deps import verify_internal_key
from app.core.config import get_settings

get_settings()  # 필수 환경변수 누락 시 여기서 기동 실패

app = FastAPI(title="Baton AI Module", docs_url=None, redoc_url=None, openapi_url=None)


@app.get("/health")
def health() -> dict:
    # 모델명·경로·키 등 내부 정보는 넣지 않는다
    return {"status": "ok"}


# /internal/* 는 전부 내부 API Key 필수. ingest·chat·삭제 라우터는 이 아래에 등록한다.
internal = APIRouter(prefix="/internal", dependencies=[Depends(verify_internal_key)])
app.include_router(internal)
