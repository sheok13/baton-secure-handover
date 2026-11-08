# AI 모듈 테스트 가이드

AI 모듈(`ai-module/`)을 내 PC에서 띄우고 지금까지 만든 기능이 잘 되는지 확인하는 방법이에요. 위에서부터 순서대로 따라 하면 됩니다.

> 작성: D(박한나) · 기준일 2026-11-08 · 담당 범위: 수집(ingest)·서빙(API)
> chat(질의 SSE)은 아직 붙지 않았어요. 붙으면 8장을 채울게요.

---

## 0. 지켜야 할 것

- API 키는 채팅, 스크린샷, 커밋 어디에도 붙여 넣지 않아요. 키가 맞는지 볼 때도 앞 4글자와 길이만 확인해요.
- `.env`, `.venv/`, `data/`는 커밋하지 않아요 (`.gitignore`에 들어 있어요).
- 테스트에 쓰는 개인정보는 `010-0000-1234`처럼 누가 봐도 가짜인 형태만 써요.
- 명령은 **항상 `ai-module` 폴더 안에서** 실행해요. 루트에서 실행하면 `No module named 'app'`가 나요.

---

## 1. 준비

### 1.1 설치할 것

| 도구 | 확인 명령 | 비고 |
|---|---|---|
| Python 3.12 | `uv python list` | uv가 알아서 받아요 |
| uv | `uv --version` | 수업 양식과 같게 사용 |
| Ollama | `ollama --version` | 개발용 임베딩 |
| bge-m3 모델 | `ollama pull bge-m3` | 약 1.2GB |
| Groq API 키 | console.groq.com → API Keys | 무료, `gsk_`로 시작 |

### 1.2 패키지 설치

```bash
cd ai-module
uv sync
```

### 1.3 `.env` 만들기 (`ai-module/.env`)

`.env.example`을 복사한 뒤 값을 채워요.

```bash
cp .env.example .env
```

```dotenv
LLM_PROVIDER=groq
LLM_MODEL=openai/gpt-oss-120b
GROQ_API_KEY=gsk_...여기에_본인_키

EMBEDDING_PROVIDER=ollama
EMBEDDING_MODEL=bge-m3
OLLAMA_BASE_URL=http://localhost:11434

VECTOR_STORE_PATH=./data/chroma/bge-m3
AI_INTERNAL_API_KEY=아래_명령으로_만든_값
```

내부 키(16자 이상)를 만드는 명령은 이거예요.

```bash
uv run python -c "import secrets; print(secrets.token_urlsafe(32))"
```

> ⚠️ uv로 내 PC에서 실행할 때는 `OLLAMA_BASE_URL=http://localhost:11434`이에요.
> Docker Compose로 띄울 때는 **루트 `.env`** 에 `http://host.docker.internal:11434`를 써요.
> ⚠️ `GROQ_API_KEY=GROQ_API_KEY=gsk_...`처럼 이름이 두 번 들어가지 않았는지 확인하세요.

키가 제대로 들어갔는지는 아래처럼 확인해요. 앞 4글자와 길이만 출력해요.

```bash
uv run python -c "from app.core.config import get_settings as g; k=g().groq_api_key.get_secret_value(); print(k[:4], len(k))"
# 기대: gsk_ 56   (길이는 키마다 조금 다를 수 있음)
```

---

## 2. 단위 테스트 (pytest)

외부 서비스(Groq, Ollama) 없이 돌아가요. 가짜 환경변수를 테스트가 직접 넣어요.

```bash
uv run pytest -v
```

**기대 결과: 23 passed**

| 파일 | 개수 | 확인하는 것 |
|---|---|---|
| `tests/test_health.py` | 5 | `/health` 200, 내부 키 없음·틀림·한글 → 401, 맞으면 200, 없는 경로 → 404 오류 모양 |
| `tests/test_config.py` | 6 | 필수 환경변수가 비면 기동 실패, `repr()`에 키가 보이지 않음 |
| `tests/test_pii_masker.py` | 12 | 주민번호·휴대폰·일반전화·계좌는 가리고, 날짜·시각·금액·연도는 그대로 둠 |

`collected 0 items`가 나오면 `ai-module` 폴더 밖에서 실행한 거예요.

---

## 3. 서버 띄우기

```bash
uv run uvicorn app.api.main:app --reload --port 8000
```

다른 터미널에서 확인해요.

```bash
curl http://localhost:8000/health
# {"status":"ok"}
```

- `/`는 404가 정상이에요. 만들지 않은 경로예요.
- Swagger(`/docs`)는 일부러 꺼 두었어요. 내부 API라서 외부에 보이지 않게 했어요.
- `.env`가 비어 있으면 서버가 켜지지 않고 `필수 환경변수가 비어 있습니다: GROQ_API_KEY`처럼 **빠진 이름**을 알려 줘요.

---

## 4. 외부 연결 확인 (모델)

### 4.1 Ollama 임베딩 (bge-m3, 1024차원)

```bash
uv run python -c "from app.core.models import get_embeddings as e; print(len(e().embed_query('테스트')))"
# 기대: 1024
```

`ConnectionError`가 나면 Ollama가 켜져 있는지, `.env`의 주소가 `localhost:11434`인지 확인해요.

### 4.2 Groq LLM

```bash
uv run python -c "from app.core.models import get_llm as l; print(l().invoke('한 단어로 인사해줘').content)"
# 기대: 짧은 인사말
```

401이 나면 키를 잘못 넣은 거예요 (1.3의 확인 명령으로 앞 4글자 확인).

---

## 5. 수집 파이프라인 확인 (서버 없이)

### 5.1 로더 + 분할

```bash
uv run python -c "
from pathlib import Path
from app.ingest.loader import load_document
from app.ingest.splitter import split_documents
p = Path('../infra/seed/docs/1.docx')   # 파일 이름은 실제 이름으로
docs = load_document(p.read_bytes(), p.name)
chunks = split_documents(docs)
print(len(docs), len(chunks), chunks[0].page_content[:80])
"
```

- PDF는 페이지마다 Document가 하나씩 나오고 `page`가 1부터 붙어요. DOCX는 `page`가 없어요.
- 예시 문서 10개는 200~260자라서 각각 청크 1개가 나와요.

### 5.2 저장·권한 필터·삭제 (`scripts/check_indexer.py`)

```bash
uv run python -m scripts.check_indexer
```

기대 결과 (가짜 영업 문서 하나로 확인):

| 항목 | 기대 |
|---|---|
| 영업(SALES) 필터로 검색 | 2건 |
| 개발(DEV) 필터로 검색 | 0건 ← 다른 부서 문서가 안 보여야 함 |
| 같은 docId로 다시 저장 | 2건 (늘어나지 않음) |
| 삭제 | 2 → 0 |

`No module named 'scripts'`가 나면 `scripts/__init__.py`가 있는지 확인해요.

---

## 6. 수집 API 확인 (서버 켠 상태)

아래 예시의 `1.docx`, `4.docx`는 자리표시예요. `infra/seed/docs/`의 실제 파일 이름으로 바꿔 쓰세요.

먼저 키를 변수에 넣어 둬요. 화면에 키가 찍히지 않게 이렇게 해요.

```bash
# macOS/Linux (Git Bash 포함)
export KEY=$(grep '^AI_INTERNAL_API_KEY=' .env | cut -d= -f2-)
```

### 6.1 문서 수집 — 정상

```bash
curl -X POST http://localhost:8000/internal/ingest \
  -H "X-Internal-Api-Key: $KEY" \
  -F "file=@../infra/seed/docs/1.docx" \
  -F "docId=1" -F "title=복리후생 안내" \
  -F "department=ALL" -F "minRank=STAFF"
# {"docId":1,"chunkCount":1,"maskedCount":0}
```

### 6.2 개인정보 마스킹

예시 문서 3·4·9·10번에는 가짜 연락처와 계좌가 들어 있어요.

```bash
curl -X POST http://localhost:8000/internal/ingest \
  -H "X-Internal-Api-Key: $KEY" \
  -F "file=@../infra/seed/docs/4.docx" \
  -F "docId=4" -F "title=영업 보고" \
  -F "department=SALES" -F "minRank=STAFF"
# {"docId":4,"chunkCount":1,"maskedCount":2}
```

| 문서 | 기대 maskedCount |
|---|---|
| 3, 4, 9, 10 | 2 (연락처 1 + 계좌 1) |
| 나머지 | 0 |

> ⚠️ `routes_ingest.py`에 마스킹이 연결되기 전 버전으로는 3·4·9·10번을 넣지 마세요. 개인정보가 그대로 벡터 DB에 저장돼요.
> 예시 문서의 주민번호는 숫자가 아니라 `[RRN Omitted]` 글자라서 건수에 들어가지 않아요.

### 6.3 문서 삭제

```bash
curl -X DELETE http://localhost:8000/internal/documents/4 -H "X-Internal-Api-Key: $KEY"
# {"docId":4,"deletedCount":1}
```

### 6.4 오류 응답 확인

모든 오류는 `{"error":{"code":"...","message":"..."}}` 모양이에요.

| 상황 | 명령 핵심 | 기대 |
|---|---|---|
| 키 없음 | 헤더 빼고 ingest | 401 `INTERNAL_AUTH_FAILED` |
| 키 틀림 | `-H "X-Internal-Api-Key: wrong"` | 401 `INTERNAL_AUTH_FAILED` |
| 부서 값 틀림 | `-F "department=영업"` | 422 `VALIDATION_FAILED` (값은 `SALES`·`DEV`·`ALL`만) |
| 직급 값 틀림 | `-F "minRank=MANAGER"` | 422 `VALIDATION_FAILED` (`STAFF`·`LEADER`만) |
| 지원 안 하는 파일 | `-F "file=@note.txt"` | 400 `FILE_INVALID_TYPE` |
| 10MB 초과 | 큰 PDF | 413 `FILE_TOO_LARGE` |
| 글자 없는 PDF (스캔본) | 이미지만 있는 PDF | 422 `INGEST_EMPTY_TEXT` |
| Ollama 꺼짐 | Ollama 끄고 ingest | 502 `UPSTREAM_FAILED` |
| 없는 경로 | `curl localhost:8000/nope` | 404 `HTTP_404` |

---

## 7. 예시 문서 10개 한 번에 넣기

`infra/seed/docs/`의 10개 문서를 부서·직급에 맞게 넣어요.

| 번호 | department | minRank | 개인정보 |
|---|---|---|---|
| 1 복리후생 | ALL | STAFF | |
| 2 비품 | ALL | STAFF | |
| 3 거래처 등록 | SALES | STAFF | ✔ |
| 4 영업보고 | SALES | STAFF | ✔ |
| 5 단가표 | SALES | LEADER | |
| 6 인센티브 | SALES | LEADER | |
| 7 개발환경 | DEV | STAFF | |
| 8 배포 | DEV | STAFF | |
| 9 서버권한 | DEV | LEADER | ✔ |
| 10 외주계약 | DEV | LEADER | ✔ |

넣은 뒤 Chroma 데이터는 `VECTOR_STORE_PATH`(기본 `./data/chroma/bge-m3`)에 남아요. 처음부터 다시 하려면 서버를 끄고 그 폴더를 지우면 돼요.

---

## 8. 질의(chat) 확인 — 준비 중

`/internal/chat`(SSE)은 E의 검색·답변 코드와 연결한 뒤 열려요. 열리면 아래로 확인할 예정이에요.

```bash
curl -N -X POST http://localhost:8000/internal/chat \
  -H "X-Internal-Api-Key: $KEY" -H "Content-Type: application/json" \
  -d '{"question":"거래처 등록 절차 알려줘","filter":{"departments":["SALES","ALL"],"ranks":["STAFF"]},"history":[]}'
```

기대 이벤트 순서: `token`(여러 번) → `sources` → `done {}`. 필터를 빼면 400 `FILTER_INVALID`가 나와야 해요.

---

## 9. 자주 나는 오류

| 증상 | 원인 | 해결 |
|---|---|---|
| `collected 0 items` | 루트에서 pytest 실행 | `cd ai-module` 후 실행 |
| `No module named 'app'` | 루트에서 실행 | `cd ai-module` |
| `AI_INTERNAL_API_KEY(16자 이상)` | `.env`에 내부 키가 비었거나 짧음 | 1.3의 명령으로 생성 |
| 서버 켤 때 `필수 환경변수가 비어 있습니다` | 이름이 적힌 변수가 비었음 | `.env`에 해당 값 채우기 |
| Ollama `ConnectionError` | 주소가 `host.docker.internal` | uv 실행 시 `localhost:11434` |
| Groq 401 | 키 줄에 `GROQ_API_KEY=`가 두 번 | 한 번만 남기기 |
| `FileNotFoundError`, `PermissionError` | 폴더 경로를 넣었거나 Windows 경로의 `\b`, `\a` | 파일까지 적기, `r"C:\..."` 사용 |
| `No module named 'scripts'` | `scripts/__init__.py` 없음 | 빈 파일 만들기 |
| `/`가 404 | 정상 | `/health` 사용 |
| `langchain-community` deprecation 경고 | 로더 패키지 이전 예정 안내 | 지금은 무시 (남은 작업) |
