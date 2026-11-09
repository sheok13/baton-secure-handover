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

SSE_HEADERS = {"Cache-Control": "no-cache", "X-Accel-Buffering": "no"}
UNAVAILABLE = {"code": "AI_UNAVAILABLE", "message": "잠시 후 다시 시도해 주세요."}

def _fixed_answer(text: str) -> list[str]:
    """LLM 없이 보내는 고정 응답: token 1건 + 빈 sources + done"""
    return [sse_event("token", {"text": text}), sse_event("sources", []), sse_event("done", {})]

async def _chat_events(req: ChatRequest) -> AsyncIterator[str]:
    try:
        # 1) 질문 단계 Injection 검사
        if check_question(req.question).blocked:
            for e in _fixed_answer(BLOCKED_MESSAGE):
                yield e
            return

     # 2) 권한 필터 검색 (Chroma·임베딩은 기다리는 작업이라 별도 작업자에서 실행)
        hits = await run_in_threadpool(retrieve, req.question, req.filter)
        if not hits:
            for e in _fixed_answer(NO_ANSWER_MESSAGE):  # LLM 미호출
                yield e
            return
        docs = [doc for doc, _score in hits]

        # 3) 답변 스트림 + 출력 검사
        history = req.history[-get_settings().max_history_messages:]
        answer = ""
        async for token in stream_answer(req.question, docs, history):
            answer += token
            if check_output(answer).blocked:
                yield sse_event("error", UNAVAILABLE)
                return
            yield sse_event("token", {"text": token})

            # 4) 출처 → 끝
        yield sse_event("sources", format_sources(docs))
        yield sse_event("done", {})
    except Exception:
        # 이미 200으로 스트림이 시작됐으므로 HTTP 오류 대신 error 이벤트로 알리고 닫는다
        logger.exception("채팅 스트림 실패")
        yield sse_event("error", UNAVAILABLE)