package com.baton.document.controller;

import com.baton.common.ApiResponse;
import com.baton.document.dto.DocumentDetailResponse;
import com.baton.document.dto.DocumentDownloadResponse;
import com.baton.document.dto.DocumentListResponse;
import com.baton.document.dto.DocumentUploadRequest;
import com.baton.document.dto.DocumentUploadResponse;
import com.baton.document.service.DocumentService;
import com.baton.document.service.DocumentUploadService;
import com.baton.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 문서함 API (API 설계서 4장) */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentUploadService documentUploadService;

    /** GET /api/documents?keyword=정산&page=0&size=10&sort=createdAt,desc */
    @GetMapping
    public ApiResponse<DocumentListResponse> list(
            @AuthenticationPrincipal AuthMember me,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(documentService.search(me, keyword, pageable));
    }

    /** POST /api/documents (multipart/form-data: file, department, minRank, title?) → 201 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> upload(
            @AuthenticationPrincipal AuthMember me,
            @RequestPart("file") MultipartFile file,
            @Valid @ModelAttribute DocumentUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(documentUploadService.upload(me, file, request)));
    }

    /** GET /api/documents/{id} */
    @GetMapping("/{id}")
    public ApiResponse<DocumentDetailResponse> detail(
            @AuthenticationPrincipal AuthMember me,
            @PathVariable Long id) {
        return ApiResponse.success(documentService.getDetail(me, id));
    }

    /** GET /api/documents/{id}/download — 파일 미전송, 성공 메시지만 */
    @GetMapping("/{id}/download")
    public ApiResponse<DocumentDownloadResponse> download(
            @AuthenticationPrincipal AuthMember me,
            @PathVariable Long id) {
        return ApiResponse.success(documentService.download(me, id), DocumentDownloadResponse.MESSAGE);
    }
}
