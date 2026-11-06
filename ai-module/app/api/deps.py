"""내부 API Key 검증 (D 소유). /internal 라우터 전체에 의존성으로 건다."""
import hmac

from fastapi import Header, HTTPException, status

from app.core.config import get_settings


def verify_internal_key(x_internal_api_key: str | None = Header(default=None)) -> None:
    expected = get_settings().ai_internal_api_key
    if not x_internal_api_key or not hmac.compare_digest(x_internal_api_key, expected):
        # 키 값은 로그·응답에 남기지 않는다
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail={"code": "INTERNAL_AUTH_FAILED", "message": "내부 인증에 실패했습니다."},
        )
