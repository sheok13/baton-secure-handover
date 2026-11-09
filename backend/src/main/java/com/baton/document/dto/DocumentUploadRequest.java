package com.baton.document.dto;

import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /documents 폼 필드 (API 설계서 4.2). file은 컨트롤러에서 @RequestPart로 따로 받는다.
 * 부서·직급 범위가 '내 권한 안'인지는 서비스(AccessPolicy.canUpload)에서 서버가 강제한다.
 */
public record DocumentUploadRequest(
        @NotNull Department department,
        @NotNull Rank minRank,
        @Size(max = 200) String title
) {
}
