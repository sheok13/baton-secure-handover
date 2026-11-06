"""AI 모듈 공용 상수 (D 소유, E도 import).

문서를 저장할 때(D: indexer)와 검색 필터를 만들 때(E: retriever)
같은 이름·같은 값을 쓰도록 한 곳에 모아 둔다.
"""
from typing import Literal, get_args

# ── 청크 메타데이터 키 (DB 설계서 7장) ──────────────────────
META_DOC_ID = "doc_id"            # int  — 삭제·출처 재검증
META_TITLE = "title"              # str  — 출처 칩에 표시
META_DEPT = "dept"                # str  — 권한 필터 (부서)
META_MIN_RANK = "min_rank"        # str  — 권한 필터 (공개 직급)
META_PAGE = "page"                # int  — 출처 페이지 (PDF만, DOCX는 키 없음)
META_CHUNK_INDEX = "chunk_index"  # int  — 청크 id·디버깅

# ── 권한 값 (DB의 CHECK 제약과 같은 값) ─────────────────────
Department = Literal["SALES", "DEV", "ALL"]
Rank = Literal["STAFF", "LEADER"]

DEPARTMENTS: tuple[str, ...] = get_args(Department)  # ("SALES", "DEV", "ALL")
RANKS: tuple[str, ...] = get_args(Rank)              # ("STAFF", "LEADER")

