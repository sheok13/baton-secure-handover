# Baton AI Module

권한 필터 RAG 기반 사내 문서 AI 플랫폼 **바톤(Baton)** 의 AI 모듈입니다.
Backend(Spring Boot)만 호출하는 **내부 FastAPI 서버**로, 문서 수집(마스킹·분할·임베딩)과 권한 필터 RAG 답변(SSE)을 담당합니다.

- 상세 계약: `docs/7.AI모듈연동명세서.md`
- 담당: D 박한나(수집·서빙: `app/api`, `app/ingest`, `app/core`) · E 이주혁(검색·생성·AI 보안: `app/rag`, `app/prompts`, `app/guard`)

## 1. 폴더 구조

```
ai-module/
├── app/
│   ├── __init__.py
│   ├── api/                  # D — FastAPI 서버
│   │   ├── __init__.py
│   │   ├── main.py           # 앱 생성, /health, /internal 라우터 등록
│   │   ├── deps.py           # X-Internal-Api-Key 검증 (hmac.compare_digest)
│   │   ├── schemas.py        # (예정) 요청·응답 Pydantic 모델, AccessFilter
│   │   ├── routes_ingest.py  # (예정) POST /internal/ingest, DELETE /internal/documents/{docId}
│   │   ├── routes_chat.py    # (예정) POST /internal/chat (SSE)
│   │   └── sse.py            # (예정) token / sources / done / error 이벤트 생성
│   ├── core/                 # D — 공용 설정
│   │   ├── __init__.py
│   │   ├── config.py         # 환경변수 → Settings, 필수 값 없으면 기동 실패
│   │   ├── models.py         # (예정) get_llm(), get_embeddings() 공급자 전환
│   │   ├── vectorstore.py    # (예정) get_vectorstore() — Chroma
│   │   └── constants.py      # (예정) 메타데이터 키·허용 값
│   ├── ingest/               # D — 수집 파이프라인
│   │   ├── __init__.py
│   │   ├── loader.py         # (예정) PDF·DOCX 로드, 페이지 번호 보존
│   │   ├── pii_masker.py     # (예정) 주민번호·연락처·계좌 마스킹
│   │   ├── splitter.py       # (예정) 800자 / 오버랩 150
│   │   └── indexer.py        # (예정) 권한 메타데이터 부착 → Chroma 저장·삭제
│   ├── rag/                  # E — retriever.py(권한 필터), chain.py(LCEL)
│   ├── prompts/              # E — system_prompt.py
│   └── guard/                # E — injection_filter.py, output_check.py
├── tests/
│   └── test_health.py        # /health, 내부 API Key 검증 테스트
├── pyproject.toml            # 의존성 정의 (uv add 로만 수정)
├── uv.lock                   # 의존성 버전 고정 (Git 커밋 필수)
├── .python-version           # 3.12
├── .dockerignore             # .venv, .env, data/ 등 이미지에서 제외
├── .gitignore                # .venv, .env, data/ 등 커밋 제외
└── Dockerfile                # F 담당
```

`(예정)` 파일은 아직 없고, 일정에 맞춰 각 기능 브랜치에서 추가합니다.
`.venv/`, `.env`, `data/`(로컬 Chroma 인덱스)는 Git에 올리지 않습니다.

## 2. 처음 실행하기

### 2.1 준비물

- [uv](https://docs.astral.sh/uv/) 설치
- Python 3.12 (없으면 `uv python install 3.12`)
- 개발용 임베딩: [Ollama](https://ollama.com) 설치 후 `ollama pull bge-m3` (수집 기능부터 필요)

### 2.2 설치

`ai-module/` 폴더에서 실행합니다.

```bash
uv lock      # uv.lock이 없을 때만 (처음 한 번)
uv sync      # .venv 생성 + 의존성 설치 (pytest·httpx 포함)
```

다른 팀원이 올린 `uv.lock`이 있으면 `uv sync`만 하면 모두 같은 버전이 설치됩니다.

### 2.3 서버 실행

```bash
uv run uvicorn app.api.main:app --reload --port 8000
```

확인:

```bash
curl http://localhost:8000/health
# {"status":"ok"}
```

`/docs`(Swagger)는 내부 API라서 꺼 두었습니다.

### 2.4 테스트

```bash
uv run pytest
```

| 테스트 | 확인 내용 |
| --- | --- |
| `test_health_without_key` | `/health`는 키 없이 200 |
| `test_internal_rejects_missing_or_wrong_key` | `/internal/*`에 키가 없거나 틀리면 401 |
| `test_internal_accepts_right_key` | 올바른 키면 통과 |

## 3. 주요 설명

### 3.1 내부 API만 제공

이 서버는 Backend만 호출합니다. Docker Compose에서 포트를 외부에 게시하지 않고(`expose: 8000`만), `/health`를 뺀 모든 `/internal/*` 요청은 `X-Internal-Api-Key` 헤더가 맞아야 합니다.

| 메서드 | 경로 | 상태 |
| --- | --- | --- |
| GET | `/health` | 구현됨 |
| POST | `/internal/ingest` | 예정 (D) |
| POST | `/internal/chat` | 예정 (D·E) |
| DELETE | `/internal/documents/{docId}` | 예정 (D) |

### 3.2 설정은 환경변수로만

모델·공급자·경로·키는 코드에 쓰지 않고 `app/core/config.py`의 `Settings`로만 읽습니다. 개발(Groq + Ollama bge-m3)과 배포(OpenAI)는 환경변수만 바꿔 전환합니다. 임베딩 모델이 바뀌면 벡터 차원이 달라지므로 `VECTOR_STORE_PATH`를 모델별로 분리하고, 배포 환경에서는 문서를 다시 적재합니다.

### 3.3 의존성 추가

`pyproject.toml`은 D가 관리합니다. 패키지가 필요하면 직접 수정하지 말고 아래 명령을 쓰거나 D에게 PR로 요청합니다.

```bash
uv add <패키지명>            # 일반 의존성
uv add --dev <패키지명>      # 테스트·개발용
```

`pyproject.toml`과 `uv.lock`이 함께 바뀌므로 두 파일을 같이 커밋합니다.

## 4. 브랜치·커밋 규칙

- 브랜치: `feature/ai-<기능>` (예: `feature/ai-init`, `feature/ai-ingest`, `feature/ai-chat-sse`), PR 대상은 `develop`
- 커밋: `chore(ai): ...`(세팅), `feat(ai): ...`(기능), `fix(ai): ...`(버그), `fix(security): ...`(보안 조치), `docs(ai): ...`(문서)
- E 부재 기간(11/7~11/10 오전, 11/12)에는 D가 `rag/`·`prompts/`·`guard/`의 연동·버그 수정만 대리 승인합니다.
