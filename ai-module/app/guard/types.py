
from dataclasses import dataclass


@dataclass(frozen=True)
class GuardResult:
    """질문·출력 점검 결과."""

    blocked: bool
    reason: str = ""