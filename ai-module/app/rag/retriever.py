from math import isfinite

from langchain_core.documents import Document

from app.api.schemas import AccessFilter
from app.core.config import get_settings
from app.core.constants import (
    DEPARTMENTS,
    RANKS,
    META_DEPT,
    META_MIN_RANK,
    META_DOC_ID,
    META_TITLE,
    META_PAGE,
)
from app.core.vectorstore import get_vectorstore


class InvalidAccessFilterError(ValueError):
    """검색에 사용할 권한 필터가 올바르지 않을 때 발생합니다."""


def build_where(f: AccessFilter) -> dict:
    """Backend의 권한 필터를 Chroma 검색 조건으로 변환합니다."""

    if f is None:
        raise InvalidAccessFilterError("권한 필터가 필요합니다.")

    if not f.departments or not f.ranks:
        raise InvalidAccessFilterError(
            "부서와 직급 목록은 비어 있을 수 없습니다."
        )

    if not set(f.departments).issubset(DEPARTMENTS):
        raise InvalidAccessFilterError("허용되지 않은 부서입니다.")

    if not set(f.ranks).issubset(RANKS):
        raise InvalidAccessFilterError("허용되지 않은 직급입니다.")

    return {
        "$and": [
            {META_DEPT: {"$in": list(f.departments)}},
            {META_MIN_RANK: {"$in": list(f.ranks)}},
        ]
    }


def retrieve(
    question: str,
    f: AccessFilter,
) -> list[tuple[Document, float]]:
    """권한 범위에서 검색하고 관련도 기준을 통과한 청크만 반환합니다."""

    # 필터가 잘못되면 벡터 스토어에 접근하기 전에 중단
    where = build_where(f)

    settings = get_settings()
    question = question.strip()

    if not question:
        raise ValueError("질문은 비어 있을 수 없습니다.")

    if len(question) > settings.max_question_chars:
        raise ValueError("질문이 최대 길이를 초과했습니다.")

    threshold = settings.relevance_threshold

    if threshold is None or not isfinite(threshold):
        raise RuntimeError(
            "유효한 RELEVANCE_THRESHOLD 설정이 필요합니다."
        )

    if settings.retriever_k < 1:
        raise RuntimeError("RETRIEVER_K는 1 이상이어야 합니다.")

    vectorstore = get_vectorstore()

    # 검색 실패를 빈 결과로 숨기지 않고 상위로 전달
    results = vectorstore.similarity_search_with_relevance_scores(
        query=question,
        k=settings.retriever_k,
        filter=where,
    )

    filtered_results = []

    for doc, score in results:
        # 비정상 점수는 '관련 문서 없음'이 아니라 검색 오류로 처리
        if not isfinite(score):
            raise RuntimeError("검색 관련도 점수가 유효하지 않습니다.")

        if score >= threshold:
            filtered_results.append((doc, score))

    return filtered_results


def select_reference_document(hits: list[tuple[Document, float]]) -> list[Document]:
    """최고 관련도 문서 하나에 속한 검색 청크만 답변 근거로 사용합니다."""
    if not hits:
        return []
    ranked = sorted(hits, key=lambda hit: hit[1], reverse=True)
    selected_id = ranked[0][0].metadata[META_DOC_ID]
    return [doc for doc, _ in ranked if doc.metadata[META_DOC_ID] == selected_id]


def format_sources(docs: list[Document]) -> list[dict]:
    """문서당 하나의 출처를 생성하며 가장 관련 있는 청크의 페이지를 사용합니다."""

    sources = []
    seen = set()

    for doc in docs:
        metadata = doc.metadata

        doc_id = metadata[META_DOC_ID]
        title = metadata[META_TITLE]
        page = metadata.get(META_PAGE)

        source_key = doc_id

        if source_key in seen:
            continue

        seen.add(source_key)

        sources.append({
            "docId": doc_id,
            "title": title,
            "page": page,
        })

    return sources
