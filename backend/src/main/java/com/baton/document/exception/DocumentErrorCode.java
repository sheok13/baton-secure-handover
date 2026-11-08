package com.baton.document.exception;

import com.baton.common.ErrorCode;
import org.springframework.http.HttpStatus;

/** 문서함 오류 코드 (API 설계서 4·7장). ErrorCode 인터페이스는 A 소유 */
public enum DocumentErrorCode implements ErrorCode {

    DOC_NOT_FOUND(HttpStatus.NOT_FOUND, "DOC_NOT_FOUND", "문서를 찾을 수 없습니다."),
    DOC_FORBIDDEN_SCOPE(HttpStatus.FORBIDDEN, "DOC_FORBIDDEN_SCOPE", "지정할 수 없는 열람 범위입니다."),
    FILE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "FILE_INVALID_TYPE", "PDF 또는 DOCX 파일만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "파일은 10MB 이하만 업로드할 수 있습니다."),
    INVALID_SORT(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "정렬은 createdAt 또는 title만 사용할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DocumentErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
