"""내부 API Key 검증 (D 소유). /internal 라우터 전체에 의존성으로 건다."""
import hmac
 
from fastapi import Header, status
 
from app.api.errors import ApiError
from app.core.config import get_settings
 
 
def verify_internal_key(x_internal_api_key: str | None = Header(default=None)) -> None:
    expected = get_settings().ai_internal_api_key.get_secret_value().encode("utf-8")
    given = (x_internal_api_key or "").encode("utf-8")
 
    # bytes끼리 비교: 헤더에 비ASCII 문자가 섞여도 TypeError(500) 없이 401로 끝난다
    if not given or not hmac.compare_digest(given, expected):
        # 키 값은 로그·응답에 남기지 않는다
        raise ApiError(status.HTTP_401_UNAUTHORIZED, "INTERNAL_AUTH_FAILED", "내부 인증에 실패했습니다.")