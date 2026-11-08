"""공통 오류 응답 (D 소유).

명세서 4.5: 모든 오류는 {"error": {"code": "...", "message": "..."}} 모양으로 응답한다.
FastAPI 기본 모양({"detail": ...})이 Backend로 나가지 않도록 여기서 한 번에 바꾼다.
오류 상세(스택 트레이스, 입력 원문)는 응답에 넣지 않는다.
"""
from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException


class ApiError(Exception):
    """우리 코드가 의도적으로 내는 오류. code는 명세서 4.5 표의 값을 쓴다."""

    def __init__(self, status_code: int, code: str, message: str) -> None:
        self.status_code = status_code
        self.code = code
        self.message = message


def _body(code: str, message: str) -> dict:
    return {"error": {"code": code, "message": message}}


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(ApiError)
    async def _api_error(_: Request, exc: ApiError) -> JSONResponse:
        return JSONResponse(status_code=exc.status_code, content=_body(exc.code, exc.message))

    @app.exception_handler(RequestValidationError)
    async def _validation_error(_: Request, exc: RequestValidationError) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content=_body("VALIDATION_FAILED", "요청 값이 올바르지 않습니다."),
        )

    @app.exception_handler(StarletteHTTPException)
    async def _http_error(_: Request, exc: StarletteHTTPException) -> JSONResponse:
        # 없는 경로(404), 허용되지 않은 메서드(405) 등 프레임워크가 내는 오류도 같은 모양으로
        return JSONResponse(status_code=exc.status_code, content=_body(f"HTTP_{exc.status_code}", "요청을 처리할 수 없습니다."))
