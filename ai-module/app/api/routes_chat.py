"""권한 필터 RAG 답변 내부 API (D 소유). /internal 아래에 붙는다.
 
흐름 (명세서 5.2): 질문 검사 → 권한 필터 검색 → (0건이면 고정 응답) → 답변 스트림
→ 출력 검사 → sources → done. 검색·답변·검사 함수는 E 소유 모듈을 부른다.
"""

import logging
from collections.abc import AsyncIterator
 
from fastapi import APIRouter
from fastapi.concurrency import run_in_threadpool
from fastapi.responses import StreamingResponse
 
from app.api.schemas import ChatRequest
from app.api.sse import sse_event
from app.core.config import get_settings
from app.guard.injection_filter import check_question
from app.guard.output_check import check_output
from app.prompts.system_prompt import BLOCKED_MESSAGE, NO_ANSWER_MESSAGE
from app.rag.chain import stream_answer
from app.rag.retriever import format_sources, retrieve

logger = logging.getLogger(__name__)
router = APIRouter()