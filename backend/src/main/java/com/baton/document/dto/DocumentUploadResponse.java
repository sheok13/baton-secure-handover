package com.baton.document.dto;

import com.baton.document.entity.DocumentStatus;

/** POST /documents 응답 201 (API 설계서 4.2) */
public record DocumentUploadResponse(Long id, DocumentStatus status, int chunkCount, int maskedCount) {
}
