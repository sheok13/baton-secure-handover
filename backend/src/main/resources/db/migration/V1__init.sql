-- ==========================================================
-- V1__init.sql — 바톤(Baton) 초기 스키마 (테이블 5개)
-- 기준: docs/3.데이터베이스설계서.md v0.4
-- ⚠️ develop 머지 후에는 이 파일을 수정하지 말 것. 변경은 V3__*.sql로 추가
-- ==========================================================

-- ----------------------------------------------------------
-- 1. members (계정) — 회원가입 없음, V2 시드로 4개 등록
-- ----------------------------------------------------------
CREATE TABLE members (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    uuid                CHAR(36)     NOT NULL,                 -- JWT subject (외부 노출 식별자)
    email               VARCHAR(100) NOT NULL,                 -- 로그인 ID
    password_hash       VARCHAR(100) NOT NULL,                 -- BCrypt 해시
    name                VARCHAR(30)  NOT NULL,
    department          VARCHAR(10)  NOT NULL,                 -- 회원은 ALL 불가
    job_rank            VARCHAR(10)  NOT NULL,                 -- rank는 예약어라 job_rank
    failed_login_count  INT          NOT NULL DEFAULT 0,       -- 연속 로그인 실패 횟수
    locked_until        DATETIME(6)     NULL,                     -- 잠금 해제 시각 (5회 실패 → +15분)
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT uk_members_uuid  UNIQUE (uuid),
    CONSTRAINT uk_members_email UNIQUE (email),
    CONSTRAINT chk_members_department CHECK (department IN ('SALES', 'DEV')),
    CONSTRAINT chk_members_job_rank   CHECK (job_rank IN ('STAFF', 'LEADER')),
    CONSTRAINT chk_members_failed_cnt CHECK (failed_login_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 2. documents (문서 메타데이터) — 원본 파일은 보관하지 않음
-- ----------------------------------------------------------
CREATE TABLE documents (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id            BIGINT       NOT NULL,                 -- 업로더
    title               VARCHAR(200) NOT NULL,                 -- 기본값: 파일명에서 확장자 제거
    original_filename   VARCHAR(255) NOT NULL,                 -- 정제 후 저장, 출력 시 이스케이프
    file_type           VARCHAR(10)  NOT NULL,
    file_size           BIGINT       NOT NULL,                 -- 바이트
    department          VARCHAR(10)  NOT NULL,                 -- 열람 부서 (ALL = 전체)
    min_rank            VARCHAR(10)  NOT NULL,                 -- 공개 직급
    status              VARCHAR(12)  NOT NULL DEFAULT 'PROCESSING',
    chunk_count         INT          NOT NULL DEFAULT 0,       -- Chroma 청크 수
    masked_count        INT          NOT NULL DEFAULT 0,       -- 마스킹된 개인정보 건수
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_documents PRIMARY KEY (id),
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id) REFERENCES members (id),
    CONSTRAINT chk_documents_file_type  CHECK (file_type IN ('PDF', 'DOCX')),
    CONSTRAINT chk_documents_file_size  CHECK (file_size > 0 AND file_size <= 10485760),
    CONSTRAINT chk_documents_department CHECK (department IN ('SALES', 'DEV', 'ALL')),
    CONSTRAINT chk_documents_min_rank   CHECK (min_rank IN ('STAFF', 'LEADER')),
    CONSTRAINT chk_documents_status     CHECK (status IN ('PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT chk_documents_counts     CHECK (chunk_count >= 0 AND masked_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_documents_access ON documents (department, min_rank, status);  -- 권한 필터 조회
CREATE INDEX idx_documents_owner  ON documents (owner_id);                       -- 업로더별 조회

-- ----------------------------------------------------------
-- 3. chat_sessions (대화)
-- ----------------------------------------------------------
CREATE TABLE chat_sessions (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    member_id           BIGINT       NOT NULL,                 -- 소유자 (IDOR 방어: 조회 시 항상 함께 검사)
    title               VARCHAR(100) NOT NULL DEFAULT '새 대화',  -- 생성 시 '새 대화', 첫 질문 저장 시 앞 30자로 갱신
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),  -- 메시지 저장 시 갱신, 목록 정렬 기준
    CONSTRAINT pk_chat_sessions PRIMARY KEY (id),
    CONSTRAINT fk_chat_sessions_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_chat_sessions_member ON chat_sessions (member_id, updated_at);  -- 내 대화 목록 최신순

-- ----------------------------------------------------------
-- 4. chat_messages (대화 메시지)
-- ----------------------------------------------------------
CREATE TABLE chat_messages (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    session_id          BIGINT       NOT NULL,
    role                VARCHAR(10)  NOT NULL,
    content             TEXT         NOT NULL,                 -- 질문 또는 답변
    sources             JSON         NULL,                     -- 재검증된 출처 JSON 문자열 [{"docId":3,"title":"...","page":2}]
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id)
        REFERENCES chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT chk_chat_messages_role CHECK (role IN ('USER', 'ASSISTANT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_chat_messages_session ON chat_messages (session_id, id);  -- 이력 시간순 조회

-- ----------------------------------------------------------
-- 5. refresh_tokens (A 파트) — 원문 미저장, SHA-256 해시만
-- ----------------------------------------------------------
CREATE TABLE refresh_tokens (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    member_id           BIGINT       NOT NULL,
    token_hash          CHAR(64)     NOT NULL,                 -- SHA-256 hex
    expires_at          DATETIME(6)     NOT NULL,
    revoked_at          DATETIME(6)     NULL,                     -- 재발급·로그아웃 시 폐기 시각
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;