package com.baton.ai.exception;

/**
 * AI 모듈 호출 실패(연결·타임아웃·4xx/5xx·응답 이상). 서비스 계층에서 잡아
 * 사용자에게는 AiErrorCode(AI_INGEST_FAILED, AI_UNAVAILABLE)로만 응답한다. 메시지는 서버 로그용.
 */
public class AiClientException extends RuntimeException {

    public AiClientException(String message) {
        super(message);
    }

    public AiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
