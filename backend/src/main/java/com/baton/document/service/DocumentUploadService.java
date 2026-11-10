package com.baton.document.service;

import com.baton.ai.dto.AiIngestResponse;
import com.baton.ai.exception.AiClientException;
import com.baton.ai.exception.AiErrorCode;
import com.baton.ai.service.AiClient;
import com.baton.common.BusinessException;
import com.baton.document.dto.DocumentUploadRequest;
import com.baton.document.dto.DocumentUploadResponse;
import com.baton.document.entity.Document;
import com.baton.document.entity.DocumentStatus;
import com.baton.document.exception.DocumentErrorCode;
import com.baton.document.repository.DocumentRepository;
import com.baton.document.service.FileValidator.ValidatedFile;
import com.baton.member.entity.Member;
import com.baton.security.AuthMember;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * 문서 업로드 (API 설계서 4.2, 아키텍처 5.1).
 * ① 범위 검증 → ② 파일 검증 → ③ PROCESSING 저장(커밋, docId 확보) → ④ AI 수집(트랜잭션 밖, 최대 120초)
 * → ⑤ READY / FAILED 저장. AI 호출 동안 DB 트랜잭션을 잡고 있지 않도록 ③·⑤를 각각 짧은 트랜잭션으로 나눈다.
 * 원본 파일은 디스크에 저장하지 않고 메모리에서 AI로만 전달한다 (원본 미보관).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadService {

    private static final int MAX_TITLE_LENGTH = 200;

    private final FileValidator fileValidator;
    private final AccessPolicy accessPolicy;
    private final DocumentRepository documentRepository;
    private final AiClient aiClient;
    private final EntityManager entityManager;
    private final PlatformTransactionManager transactionManager;

    public DocumentUploadResponse upload(AuthMember me, MultipartFile file, DocumentUploadRequest request) {
        // ① 부서·공개 직급 범위: ALL 또는 내 부서, 사원은 STAFF만 (요청 값을 믿지 않고 DB 회원 기준으로 판단)
        if (!accessPolicy.canUpload(me.department(), me.rank(), request.department(), request.minRank())) {
            throw new BusinessException(DocumentErrorCode.DOC_FORBIDDEN_SCOPE);
        }

        // ② 확장자·MIME·매직넘버·크기·파일명
        ValidatedFile vf = fileValidator.validate(file);
        String title = resolveTitle(request.title(), vf.baseName());

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // ③ PROCESSING 저장 → 커밋 (docId를 Chroma 메타데이터에 쓰기 위해 먼저 확보)
        Document saved = tx.execute(status -> {
            Member ownerRef = entityManager.getReference(Member.class, me.id());
            return documentRepository.save(Document.create(
                    ownerRef, title, vf.filename(), vf.type(), vf.size(),
                    request.department(), request.minRank()));
        });
        Long docId = saved.getId();

        // ④ AI 수집 (트랜잭션 밖)
        AiIngestResponse result;
        try {
            result = aiClient.ingest(docId, title, request.department(), request.minRank(), vf.type(), vf.content());
        } catch (AiClientException e) {
            log.warn("문서 수집 실패 docId={} reason={}", docId, e.getMessage());
            tx.executeWithoutResult(status ->
                    documentRepository.findById(docId).ifPresent(Document::markFailed));
            aiClient.deleteChunks(docId);   // AI도 실패 시 지우지만, 타임아웃 등으로 남았을 수 있어 한 번 더 정리
            throw new BusinessException(AiErrorCode.AI_INGEST_FAILED);
        }

        // ⑤ READY 저장
        tx.executeWithoutResult(status ->
                documentRepository.findById(docId)
                        .ifPresent(d -> d.markReady(result.chunkCount(), result.maskedCount())));
        log.info("문서 업로드 완료 docId={} memberUuid={} chunks={} masked={}",
                docId, me.uuid(), result.chunkCount(), result.maskedCount());

        return new DocumentUploadResponse(docId, DocumentStatus.READY, result.chunkCount(), result.maskedCount());
    }

    /** 제목 미입력 → 파일명(확장자 제외). 입력값·기본값 모두 앞뒤 공백 제거, 200자 이내 */
    static String resolveTitle(String requested, String baseName) {
        String t = (requested == null || requested.isBlank()) ? baseName : requested;
        t = t.strip();
        return t.length() > MAX_TITLE_LENGTH ? t.substring(0, MAX_TITLE_LENGTH) : t;
    }
}
