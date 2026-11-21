
import re
import unicodedata

from .types import GuardResult


# 차단 사유 코드와 탐지 패턴
QUESTION_RULES = (
    (
        "IGNORE_INSTRUCTIONS",
        re.compile(
            r"(?:이전|기존|위의|앞의|모든)\s*"
            r"(?:지시(?:사항)?|명령|규칙)"
            r".{0,30}?(?:무시|잊어|따르지\s*마)"
            r"|(?:ignore|disregard|forget)\b.{0,40}"
            r"\b(?:instructions?|rules?)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "REVEAL_SYSTEM_PROMPT",
        re.compile(
            r"(?:시스템\s*프롬프트|내부\s*지침|개발자\s*메시지)"
            r".{0,30}?(?:보여|알려|공개|출력|복사)"
            r"|(?:show|reveal|print|repeat)\b.{0,40}"
            r"\b(?:system\s*prompt|developer\s*message)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "BYPASS_ACCESS_CONTROL",
        re.compile(
            r"(?:권한|접근\s*제한|보안\s*규칙)"
            r".{0,20}?(?:무시|우회|해제)"
            r"|(?:bypass|ignore|disable)\b.{0,30}"
            r"\b(?:permissions?|access\s*control|security\s*rules?)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "REQUEST_SECRET",
        re.compile(
            r"(?:API\s*키|비밀\s*키|인증\s*토큰)"
            r".{0,20}?(?:보여|알려|공개|출력)"
            r"|(?:show|reveal|print)\b.{0,30}"
            r"\b(?:api\s*key|secret\s*key|access\s*token)\b",
            re.IGNORECASE,
        ),
    ),
)


def check_question(question: str) -> GuardResult:
    """질문의 명시적인 지시 우회·내부 정보 공개 요청을 점검합니다."""

    # 전각 문자 등을 정규화
    normalized = unicodedata.normalize("NFKC", question)

    # 줄바꿈을 포함한 연속 공백을 공백 하나로 변환
    normalized = re.sub(r"\s+", " ", normalized).strip()

    if not normalized:
        raise ValueError("질문은 비어 있을 수 없습니다.")

    for reason, pattern in QUESTION_RULES:
        if pattern.search(normalized):
            return GuardResult(
                blocked=True,
                reason=reason,
            )

    return GuardResult(blocked=False)