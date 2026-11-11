"""관련도 점수 확인 (임계값 정할 때만 사용). 저장소를 읽기만 하고 바꾸지 않는다.

사용: uv run python scripts/score_check.py dev_staff "사내 복리후생이 뭐야?" "여름휴가는 며칠이야?"
"""
import sys

import sys
from pathlib import Path

from app.core.config import get_settings
from app.core.vectorstore import get_vectorstore

# 파일 경로로 실행해도 app 패키지를 찾도록 ai-module 폴더를 검색 경로에 추가
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

# 권한 테스트와 같은 계정별 필터 (Backend AiFilter 규칙)
ACCOUNTS = {
    "sales_staff": (["SALES", "ALL"], ["STAFF"]),
    "sales_lead": (["SALES", "ALL"], ["STAFF", "LEADER"]),
    "dev_staff": (["DEV", "ALL"], ["STAFF"]),
    "dev_lead": (["DEV", "ALL"], ["STAFF", "LEADER"]),
}


def main() -> None:
    if len(sys.argv) < 3 or sys.argv[1] not in ACCOUNTS:
        print("사용: score_check.py <" + "|".join(ACCOUNTS) + "> \"질문1\" \"질문2\" ...")
        sys.exit(1)

    depts, ranks = ACCOUNTS[sys.argv[1]]
    where = {"$and": [{"dept": {"$in": depts}}, {"min_rank": {"$in": ranks}}]}
    s = get_settings()
    th = s.relevance_threshold
    print(f"임베딩 {s.embedding_provider}/{s.embedding_model} · 현재 기준 {th}")

    store = get_vectorstore()
    for q in sys.argv[2:]:
        print(f"\n질문: {q}")
        hits = store.similarity_search_with_relevance_scores(q, k=4, filter=where)
        if not hits:
            print("  (권한 안에서 검색된 조각 없음)")
        for doc, score in hits:
            mark = "통과" if th is not None and score >= th else "탈락"
            m = doc.metadata
            print(f"  {score:.3f}  {mark}  {m.get('title')} p.{m.get('page', '-')} #{m.get('chunk_index')}")


if __name__ == "__main__":
    main()