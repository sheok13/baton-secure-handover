"""SSE 이벤트 문자열 만들기 (D 소유).

형식 (API 설계서 5.1·6.2):
    event: token
    data: {"text":"..."}
    (빈 줄)
"""
import json


def sse_event(event: str, data) -> str:
    # ensure_ascii=False: 한글을 \uXXXX로 바꾸지 않고 그대로 보낸다
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"
