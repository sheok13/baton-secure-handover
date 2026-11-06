"""문서 수집·삭제 내부 API (D 소유). /internal 아래에 붙는다 (키 검증은 main.py에서)."""
import logging

from fastapi import APIRouter, File, Form, Path, UploadFile

from app.api.errors import ApiError
from app.api.schemas import DeleteResponse, IngestResponse
from app.core.config import get_settings
from app.core.constants import Department, Rank
from app.ingest.indexer import delete_document, index_chunks
from app.ingest.loader import EmptyTextError, UnsupportedFileTypeError, load_document
from app.ingest.splitter import split_documents

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/ingest", response_model=IngestResponse)
def ingest(
    file: UploadFile = File(...),
    docId: int = Form(..., ge=1),
    title: str = Form(..., min_length=1, max_length=200),
    department: Department = Form(...),
    minRank: Rank = Form(...),
) -> IngestResponse:
    # 1) 크기 재확인 (Backend 검증 후 2차 방어선) — 한도+1바이트까지만 읽는다
    max_bytes = get_settings().max_upload_mb * 1024 * 1024
    data = file.file.read(max_bytes + 1)
    if len(data) > max_bytes:
        raise ApiError(413, "FILE_TOO_LARGE", "파일이 너무 큽니다.")

    # 2) 글자 꺼내기
    try:
        docs = load_document(data, file.filename or "")
    except UnsupportedFileTypeError:
        raise ApiError(400, "FILE_INVALID_TYPE", "PDF·DOCX만 지원합니다.")
    except EmptyTextError:
        raise ApiError(422, "INGEST_EMPTY_TEXT", "문서에서 텍스트를 찾을 수 없습니다.")
    except Exception:
        logger.exception("문서 파싱 실패 docId=%s", docId)
        raise ApiError(422, "INGEST_EMPTY_TEXT", "문서를 읽을 수 없습니다.")

    # 3) TODO(11/7): 개인정보 마스킹 — 분할 전에 적용하고 masked_count 집계
    masked_count = 0

    # 4) 자르기 → 이름표 붙여 저장
    chunks = split_documents(docs)
    try:
        count = index_chunks(
            doc_id=docId, title=title, department=department, min_rank=minRank, chunks=chunks
        )
    except Exception:
        logger.exception("임베딩·저장 실패 docId=%s", docId)
        raise ApiError(502, "UPSTREAM_FAILED", "문서 저장에 실패했습니다.")

    logger.info("문서 수집 완료 docId=%s chunks=%s", docId, count)
    return IngestResponse(docId=docId, chunkCount=count, maskedCount=masked_count)


@router.delete("/documents/{doc_id}", response_model=DeleteResponse)
def delete(doc_id: int = Path(..., ge=1)) -> DeleteResponse:
    return DeleteResponse(docId=doc_id, deletedCount=delete_document(doc_id))