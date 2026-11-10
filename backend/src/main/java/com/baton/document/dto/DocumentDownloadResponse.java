package com.baton.document.dto;

import com.baton.document.entity.Document;
import com.baton.document.entity.FileType;

/** GET /documents/{id}/download 응답 (API 설계서 4.4). 원본 미보관 원칙에 따라 파일은 보내지 않는다 */
public record DocumentDownloadResponse(Long id, String title, FileType fileType) {

    public static final String MESSAGE = "다운로드되었습니다.";

    public static DocumentDownloadResponse from(Document d) {
        return new DocumentDownloadResponse(d.getId(), d.getTitle(), d.getFileType());
    }
}
