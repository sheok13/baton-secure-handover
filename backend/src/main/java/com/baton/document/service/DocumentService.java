package com.baton.document.service;

import com.baton.ai.service.AiFilter;
import com.baton.common.BusinessException;
import com.baton.document.dto.DocumentDetailResponse;
import com.baton.document.dto.DocumentDownloadResponse;
import com.baton.document.dto.DocumentListResponse;
import com.baton.document.entity.Document;
import com.baton.document.entity.DocumentStatus;
import com.baton.document.exception.DocumentErrorCode;
import com.baton.document.repository.DocumentRepository;
import com.baton.security.AuthMember;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서함 조회 (API 설계서 4.1·4.3·4.4).
 * 권한은 AuthMember(= JWT uuid로 DB에서 조회한 회원)의 부서·직급으로만 판단한다. 요청 값은 쓰지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    /** sort 화이트리스트 — 그 외 필드는 400 (SQL Injection·내부 컬럼 노출 방지) */
    private static final Set<String> SORTABLE = Set.of("createdAt", "title");
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final DocumentRepository documentRepository;
    private final AccessPolicy accessPolicy;

    /** GET /documents — 권한 밖 문서는 목록·건수 모두 제외 */
    public DocumentListResponse search(AuthMember me, String keyword, Pageable pageable) {
        AiFilter filter = AiFilter.of(me.department(), me.rank());
        Pageable safe = sanitize(pageable);
        return DocumentListResponse.from(
                documentRepository.searchReadable(
                        DocumentStatus.READY,
                        filter.departments(),
                        filter.ranks(),
                        normalizeKeyword(keyword),
                        safe));
    }

    /** GET /documents/{id} — 없음·권한 밖·READY 아님은 모두 404 */
    public DocumentDetailResponse getDetail(AuthMember me, Long id) {
        return DocumentDetailResponse.from(findReadable(me, id));
    }

    /** GET /documents/{id}/download — 권한 확인 + 감사 로그. 파일은 보내지 않는다 (원본 미보관) */
    public DocumentDownloadResponse download(AuthMember me, Long id) {
        Document doc = findReadable(me, id);
        // TODO(A): AuditLogger 머지 후 auditLogger.docDownload(me.uuid(), doc.getId())로 교체
        log.info("[AUDIT] DOC_DOWNLOAD memberUuid={} docId={}", me.uuid(), doc.getId());
        return DocumentDownloadResponse.from(doc);
    }

    /** 열람 가능한 문서만 반환. 존재 여부를 숨기기 위해 '없음'과 '권한 없음'을 같은 404로 응답 */
    Document findReadable(AuthMember me, Long id) {
        Document doc = documentRepository.findWithOwnerById(id).orElse(null);
        if (doc == null || !accessPolicy.canRead(me.department(), me.rank(), doc)) {
            if (doc != null) {
                // TODO(A): AuditLogger 머지 후 권한 밖 접근 감사 로그로 교체 (서버 로그에만 남고 응답은 404)
                log.warn("[AUDIT] DOC_ACCESS_DENIED memberUuid={} docId={}", me.uuid(), id);
            }
            throw new BusinessException(DocumentErrorCode.DOC_NOT_FOUND);
        }
        return doc;
    }

    /** sort 화이트리스트 검사 + 페이지 크기 상한 */
    Pageable sanitize(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw new BusinessException(DocumentErrorCode.INVALID_SORT);
            }
        }
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    /**
     * 공백 → null(전체), 100자 초과 → 400.
     * LIKE 와일드카드(%, _)와 이스케이프 문자(\)는 글자 그대로 검색되도록 이스케이프한다 (MariaDB 기본 ESCAPE = '\').
     */
    String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String k = keyword.strip();
        if (k.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException(DocumentErrorCode.INVALID_KEYWORD);
        }
        return k.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
