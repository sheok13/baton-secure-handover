# Baton

**부서·직급에 따른 문서 접근 제어와 AI 질의응답을 제공하는 보안 인수인계 서비스**입니다.

인수인계 자료를 찾기 위해 여러 문서를 직접 확인하는 과정을 줄이고, 로그인한 사용자가 열람할 수 있는 문서 안에서 업무에 필요한 답을 제공합니다.

## 주요 기능

- **문서 관리:** PDF·DOCX 업로드, 문서 목록 조회 및 권한별 접근 제어
- **문서 기반 채팅:** 접근 가능한 문서 검색, 관련 문서 하나를 근거로 답변, 참고 문서 표시
- **대화 관리:** 새 대화 생성, 답변 스트리밍, 이전 대화 조회 및 오류 재시도
- **보안 처리:** JWT 인증, 검색 전 부서·직급 필터링, 개인정보 마스킹, 출처 권한 재검증

## 기술 구성

| 영역 | 기술 |
|---|---|
| Frontend | React, Vite, Tailwind CSS, Zustand, react-markdown |
| Backend | Java 17, Spring Boot, Spring Security, JPA, Flyway |
| AI | Python 3.12, FastAPI, LangChain, Chroma |
| 모델 | Groq / OpenAI LLM, Ollama / OpenAI 임베딩 |
| 데이터·배포 | MariaDB, Docker Compose, Nginx |

```text
사용자 → Frontend → Backend → AI 모듈 → 문서 검색·답변 생성
                       │         └─ Chroma: 문서 청크·벡터
                       └─ MariaDB: 회원·문서 정보·대화 기록
```

## 프로젝트 구조

```text
baton-secure-handover/
├─ frontend/     로그인·문서함·채팅 화면
├─ backend/      인증·문서·채팅 API와 DB 마이그레이션
├─ ai-module/    문서 수집·검색·답변 생성·보안 검사
├─ infra/        Docker·Nginx 설정, 시드 문서와 등록 스크립트
└─ docs/         프로젝트 계획서·설계서·개발 명세서
```

## 시작하기 — 로컬 실행

아래는 **Windows PowerShell에서 Docker 없이 실행하는 방법**입니다. Java 17, Node.js 22, Python 3.12, uv, MariaDB, Ollama를 먼저 준비하세요. LLM 공급자의 API 키와 사용 가능한 모델 ID도 필요합니다.

### 1. 저장소 내려받기

```powershell
git clone https://github.com/sheok13/baton-secure-handover.git
cd baton-secure-handover
```

### 2. 환경설정

루트의 [`.env.example`](.env.example)을 참고해 아래 파일을 준비합니다. 기존 설정 파일이 있다면 덮어쓰지 말고 필요한 값만 확인하세요.

| 파일 | 설정할 주요 항목 |
|---|---|
| `.env` | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET_KEY`, `AI_INTERNAL_API_KEY`, `AI_BASE_URL`, `CORS_ALLOWED_ORIGINS` |
| `ai-module/.env` | `AI_INTERNAL_API_KEY`, `LLM_PROVIDER`, `LLM_MODEL`, 공급자 API 키, `EMBEDDING_PROVIDER`, `EMBEDDING_MODEL`, `OLLAMA_BASE_URL`, `VECTOR_STORE_PATH`, `CHROMA_COLLECTION`, `RELEVANCE_THRESHOLD` |
| `frontend/.env` | `VITE_API_BASE_URL=/api`, `VITE_USE_MOCK=false` |

로컬 개발 시 주소와 모델 설정은 다음을 기준으로 맞춥니다.

- DB: `jdbc:mariadb://127.0.0.1:3306/baton_db` — 별도 포트를 사용한다면 변경
- Backend의 `AI_BASE_URL`: `http://127.0.0.1:8000`
- `CORS_ALLOWED_ORIGINS`: `http://localhost:5173,http://127.0.0.1:5173`
- 개발 모델 구성: `LLM_PROVIDER=groq`, `EMBEDDING_PROVIDER=ollama`, `EMBEDDING_MODEL=bge-m3`
- `OLLAMA_BASE_URL`: `http://127.0.0.1:11434`
- `VECTOR_STORE_PATH`: `./data/chroma/bge-m3`, `CHROMA_COLLECTION`: `baton_docs`
- `RELEVANCE_THRESHOLD`: bge-m3 로컬 검증 설정은 `0.5`; 모델 변경 시 재검증 필요

`LLM_MODEL`과 공급자 API 키는 직접 지정합니다. Backend와 AI의 `AI_INTERNAL_API_KEY`는 같아야 하며 16자 이상으로 설정합니다. `JWT_SECRET_KEY`는 별도의 32바이트 이상 값으로 설정합니다. 실제 키·비밀번호와 `.env`는 커밋하지 않습니다.

### 3. DB와 Ollama 준비

MariaDB에 `baton_db`와 해당 DB를 사용할 계정을 준비하고 `.env`에 접속 정보를 설정합니다. Backend가 시작되면 Flyway가 테이블과 시연 계정 마이그레이션을 적용합니다.

```powershell
ollama pull bge-m3
ollama list
```

Ollama가 실행 중이 아니라면 별도 터미널에서 `ollama serve`를 실행합니다. 이미 실행 중이면 다시 실행할 필요가 없습니다.

### 4. 서버 실행

각 서버는 **별도 터미널**에서 실행합니다. 아래 `cd` 명령은 저장소 루트를 기준으로 합니다.

**AI 서버**

```powershell
cd ai-module
uv sync --frozen
.\.venv\Scripts\python.exe -m uvicorn app.api.main:app --host 127.0.0.1 --port 8000 --reload
```

AI는 실행 디렉터리의 `.env`를 읽습니다. `Application startup complete` 로그가 나오면 기동된 것입니다.

**Backend**

Backend는 `.env`를 자동으로 읽지 않으므로, 저장소 루트에서 다음 명령으로 환경변수를 주입합니다. `.env` 값은 따옴표·인라인 주석 없는 `KEY=value` 형식으로 작성하세요.

```powershell
Get-Content -LiteralPath .\.env -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^(SPRING_DATASOURCE_URL|SPRING_DATASOURCE_USERNAME|SPRING_DATASOURCE_PASSWORD|JWT_SECRET_KEY|AI_INTERNAL_API_KEY|AI_BASE_URL|CORS_ALLOWED_ORIGINS)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim(), 'Process')
    }
}

# JAVA_HOME을 실제 Java 17 설치 경로로 설정한 뒤 같은 터미널에서 실행
cd backend
& "$env:JAVA_HOME\bin\java.exe" '-Dfile.encoding=UTF-8' `
  -classpath gradle/wrapper/gradle-wrapper.jar `
  org.gradle.wrapper.GradleWrapperMain bootJar --no-daemon

# BUILD SUCCESSFUL 확인 후 실행
& "$env:JAVA_HOME\bin\java.exe" '-Dfile.encoding=UTF-8' -jar build/libs/app.jar
```

**Frontend**

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev -- --host 127.0.0.1 --port 5173 --strictPort
```

| 확인 대상 | 주소 |
|---|---|
| 웹 화면 | http://localhost:5173 |
| Backend 상태 | http://localhost:8080/actuator/health |
| AI 상태 | http://127.0.0.1:8000/health |

Frontend는 `/api` 요청을 Backend의 `8080` 포트로 전달합니다. 서버는 터미널을 열어 둔 상태로 사용하고, 종료할 때 `Ctrl+C`를 누릅니다.

Docker로 실행하려면 [컨테이너 배포 가이드](docs/8.컨테이너배포가이드.md)와 [Compose 설정](infra/docker-compose.yml)을 참고하세요. 로컬과 컨테이너의 DB·AI·Ollama 주소는 서로 다릅니다.

## 사용 방법

1. 계정으로 로그인합니다.
2. 문서를 업로드하거나 아래 시드 등록 스크립트를 실행합니다.
3. 문서가 `READY` 상태인지 확인합니다.
4. **새 대화**에서 질문하고 답변 아래의 참고 문서를 확인합니다.

### 시연 계정

| 이메일 | 부서 | 직급 |
|---|---|---|
| `sales.staff@baton.local` | 영업 | 사원 |
| `sales.lead@baton.local` | 영업 | 팀장 |
| `dev.staff@baton.local` | 개발 | 사원 |
| `dev.lead@baton.local` | 개발 | 팀장 |

비밀번호는 팀에서 공유받은 값 또는 해당 로컬 DB에 설정한 값을 사용합니다. 공용 문서는 양쪽 부서에서 조회할 수 있고, 부서 문서와 팀장 문서는 해당 권한을 가진 계정만 조회할 수 있습니다.

## 테스트

```powershell
# ai-module 폴더: 출처 선택·제거, 스트리밍, API 회귀 테스트
uv run pytest tests/test_reference_stream.py tests/test_answer_reference.py tests/test_chat_api.py -v

# frontend 폴더: 빌드 확인
npm.cmd run build

# backend 폴더: 단위 테스트
.\gradlew.bat test
```

Backend Wrapper가 한글 경로에서 실패한다면 위 Backend 실행 절차의 Java 직접 호출 명령에서 `bootJar`를 `test`로 바꿔 실행하세요.

AI 단위 테스트는 외부 모델 호출을 대체합니다. 실제 문서 검색·답변과 화면 표시는 별도로 확인해야 합니다. 권한별 질문 예시는 [권한 테스트 시나리오](infra/seed/permission_cases.md)를 참고하세요.

## 실행 시 확인할 사항

- **포트 충돌:** `netstat -ano`로 8000·8080·5173을 사용하는 프로세스를 확인합니다. 다른 프로젝트의 서버가 실행 중일 수 있습니다.
- **관련 문서를 찾지 못함:** 문서의 `READY` 상태, 계정 권한, AI 벡터 저장 경로와 검색 임계값을 확인합니다.
- **채팅 연결 실패:** Backend·AI·Ollama 실행 여부와 Backend·AI의 내부 키 일치를 확인합니다.
- **환경변수 누락:** 실행 폴더와 설정 파일 위치를 확인합니다. Backend 환경변수는 서버를 실행할 터미널에 주입해야 합니다.

## 상세 문서

| 문서 | 내용 |
|---|---|
| [프로젝트 계획서](docs/1.프로젝트계획서.md) | 목적·범위·추진 계획 |
| [시스템 아키텍처](docs/2.시스템아키텍처설계서.md) | 서비스 구성·처리 흐름 |
| [데이터베이스 설계](docs/3.데이터베이스설계서.md) | 테이블·관계·데이터 정책 |
| [API 설계](docs/4.API설계서.md) | 요청·응답 및 오류 계약 |
| [Backend 명세](docs/5.Backend개발명세서.md) | 인증·문서·채팅 서버 구현 |
| [Frontend 명세](docs/6.Frontend개발명세서.md) | 화면·상태·API 연동 |
| [AI 모듈 명세](docs/7.AI모듈연동명세서.md) | 수집·검색·생성·보안 처리 |
| [배포 가이드](docs/8.컨테이너배포가이드.md) | 컨테이너 구성과 배포 |
