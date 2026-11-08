"""개인정보 마스킹 (D 소유).
 
임베딩 전에 주민등록번호 → 연락처 → 계좌번호 순으로 가린다 (명세서 5.1 ②).
순서가 중요하다: 주민번호(13자리)를 먼저 가려야 계좌 규칙에 잘못 걸리지 않는다.
"""
import re
 
RRN_MASK = "[주민번호]"
PHONE_MASK = "[연락처]"
ACCOUNT_MASK = "[계좌번호]"


# (?<!\d) / (?!\d): 앞뒤에 숫자가 붙어 있으면 더 긴 숫자의 일부이므로 건드리지 않는다
_RRN = re.compile(r"(?<!\d)\d{6}-?[1-4]\d{6}(?!\d)")
_MOBILE = re.compile(r"(?<!\d)01[016789]-?\d{3,4}-?\d{4}(?!\d)")
_LANDLINE = re.compile(r"(?<!\d)0\d{1,2}-\d{3,4}-\d{4}(?!\d)")
# 하이픈으로 이어진 숫자 묶음 (예: 999-99-999999). 자릿수 판단은 _mask_account에서
_DIGIT_GROUPS = re.compile(r"(?<![\d-])\d{2,6}(?:-\d{2,6}){1,3}(?![\d-])")

def _mask_account(text: str) -> tuple[str, int]:
    count = 0
 
    def repl(m: re.Match) -> str:
        nonlocal count
        digits = sum(ch.isdigit() for ch in m.group())
        if 10 <= digits <= 14:  # 계좌번호 길이. 날짜(2026-11-05, 8자리)는 여기서 빠진다
            count += 1
            return ACCOUNT_MASK
        return m.group()
 
    return _DIGIT_GROUPS.sub(repl, text), count