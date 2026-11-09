"""chat SSE 라우트 테스트 (D 소유).
 
E의 검색·답변·검사 함수는 가짜로 바꿔 끼운다(monkeypatch).
그래서 Ollama·Groq·Chroma 없이도 D가 만든 '받기 → 순서대로 호출 → SSE 보내기'만 검사할 수 있다.
"""