"""FastAPI 진입점 (D 소유).

실행: uv run uvicorn app.api.main:app --reload --port 8000
"""
import logging

from fastapi import APIRouter, Depends, FastAPI
from fastapi.responses import JSONResponse

from app.api.deps import verify_internal_key
from app.api.errors import register_error_handlers
from app.api.routes_chat import router as chat_router
from app.api.routes_ingest import router as ingest_router
from app.core.config import get_settings
from app.core.vectorstore import get_vectorstore

get_settings()  # 필수 환경변수 누락 시 여기서 기동 실패
logger = logging.getLogger(__name__)

app = FastAPI(title="Baton AI Module", docs_url=None, redoc_url=None, openapi_url=None)
register_error_handlers(app)


@app.get("/health")
def health():
    # 벡터 스토어를 열 수 없으면 503 (명세서 4.4). 모델명·경로 등 내부 정보는 응답에 넣지 않는다.
    try:
        get_vectorstore().get(limit=1)  # 저장된 조각 1개만 읽어 본다 (임베딩 AI는 부르지 않음)
    except Exception:
        logger.exception("헬스체크 실패: 벡터 스토어 열기 실패")
        return JSONResponse(status_code=503, content={"status": "unavailable"})
    return {"status": "ok"}


# /internal/* 는 전부 내부 API Key 필수
internal = APIRouter(prefix="/internal", dependencies=[Depends(verify_internal_key)])
internal.include_router(ingest_router)
internal.include_router(chat_router)
app.include_router(internal)