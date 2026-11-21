
import re
import unicodedata

from .types import GuardResult


OUTPUT_RULES = (
    (
        "RESIDENT_ID_PATTERN",
        re.compile(
            r"(?<!\d)\d{6}\s*[-‐‑–—]?\s*[1-4]\d{6}(?!\d)"
        ),
    ),
    (
        "PHONE_NUMBER_PATTERN",
        re.compile(
            r"(?<!\d)(?:"
            r"01[016789][ -]?\d{3,4}[ -]?\d{4}"
            r"|0\d{1,2}-\d{3,4}-\d{4}"
            r")(?!\d)"
        ),
    ),
    (
        "API_KEY_PATTERN",
        re.compile(
            r"(?<![A-Za-z0-9_])"
            r"(?:sk-[A-Za-z0-9_-]{20,}|gsk_[A-Za-z0-9]{20,})"
            r"(?![A-Za-z0-9_])"
        ),
    ),
    (
        "UNSAFE_HTML_PATTERN",
        re.compile(
            r"<\s*/?\s*(?:script|iframe|object|embed)\b"
            r"|<[^>]*\bon[a-z]+\s*="
            r"|javascript\s*:",
            re.IGNORECASE,
        ),
    ),
)

# 명세서의 계좌 마스킹 기준:
# 하이픈으로 나뉜 숫자 묶음이며 숫자 합계가 10~14자리
ACCOUNT_CANDIDATE = re.compile(
    r"(?<![\w-])\d+(?:-\d+)+(?![\w-])"
)

# 날짜를 계좌번호로 오인하지 않도록 제외
DATE_PATTERN = re.compile(
    r"\d{4}-\d{1,2}-\d{1,2}"
)


def check_output(text_so_far: str) -> GuardResult:
    """지금까지 생성된 답변에서 민감정보·위험 출력 패턴을 점검합니다."""

    if not text_so_far:
        return GuardResult(blocked=False)

    normalized = unicodedata.normalize("NFKC", text_so_far)

    # 주민번호, 전화번호, 일부 API 키 형태, 위험 HTML 점검
    for reason, pattern in OUTPUT_RULES:
        if pattern.search(normalized):
            return GuardResult(
                blocked=True,
                reason=reason,
            )

    # 계좌번호 후보 점검
    for match in ACCOUNT_CANDIDATE.finditer(normalized):
        candidate = match.group()

        if DATE_PATTERN.fullmatch(candidate):
            continue

        digit_count = sum(char.isdigit() for char in candidate)

        if 10 <= digit_count <= 14:
            return GuardResult(
                blocked=True,
                reason="ACCOUNT_NUMBER_PATTERN",
            )

    return GuardResult(blocked=False)