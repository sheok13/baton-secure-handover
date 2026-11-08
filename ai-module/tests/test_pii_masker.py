"""pii_masker 단위 테스트. 값은 모두 누가 봐도 가짜인 형태만 쓴다."""
import pytest

from app.ingest.pii_masker import mask


@pytest.mark.parametrize(
    "text, expected, count",
    [
        # 주민등록번호 (하이픈 있음/없음)
        ("식별번호: 000000-1000000", "식별번호: [주민번호]", 1),
        ("식별번호 0000001000000 입니다", "식별번호 [주민번호] 입니다", 1),
        # 휴대전화·일반전화
        ("연락처: 010-0000-1234", "연락처: [연락처]", 1),
        ("연락처 01000001234", "연락처 [연락처]", 1),
        ("대표번호 02-000-0000", "대표번호 [연락처]", 1),
        # 계좌번호
        ("계좌: 테스트은행 999-99-999999", "계좌: 테스트은행 [계좌번호]", 1),
        # 예시 문서 4번과 같은 한 문장에 여러 건
        (
            "비상 연락망(010-0000-1234), 법인 계좌(테스트은행 999-99-999999)",
            "비상 연락망([연락처]), 법인 계좌(테스트은행 [계좌번호])",
            2,
        ),
    ],
)
def test_masks_personal_info(text, expected, count):
    assert mask(text) == (expected, count)


@pytest.mark.parametrize(
    "text",
    [
        "시행일: 2026-11-01",                    # 날짜
        "매주 화요일 10:00 정기 배포",            # 시각
        "건당 50만 원, 월 매출 1억 이상",         # 금액
        "정가 대비 35% 할인, 2026년 하반기",       # 숫자·연도
        "개인 식별번호: [RRN Omitted]",           # 예시 문서의 자리표시
    ],
)
def test_keeps_non_personal_numbers(text):
    assert mask(text) == (text, 0)