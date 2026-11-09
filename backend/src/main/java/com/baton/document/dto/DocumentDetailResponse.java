package com.baton.document.dto;

import com.baton.document.entity.Document;
import com.baton.document.entity.DocumentStatus;
import com.baton.document.entity.FileType;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;

/** GET /documents/{id} 응답 (API 설계서 4.3) */
public record DocumentDetailResponse(
        Long id,
        String title,
        String originalFilename,
        FileType fileType,
        long fileSize,
        AuthorResponse author,
        Department department,
        Rank minRank,
        DocumentStatus status,
        int chunkCount,
        int maskedCount,
        String createdAt
) {

    public static DocumentDetailResponse from(Document d) {
        return new DocumentDetailResponse(
                d.getId(),
                d.getTitle(),
                d.getOriginalFilename(),
                d.getFileType(),
                d.getFileSize(),
                AuthorResponse.from(d.getOwner()),
                d.getDepartment(),
                d.getMinRank(),
                d.getStatus(),
                d.getChunkCount(),
                d.getMaskedCount(),
                KstTime.format(d.getCreatedAt())
        );
    }
}
