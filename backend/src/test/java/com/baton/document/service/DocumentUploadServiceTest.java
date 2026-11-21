package com.baton.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baton.ai.dto.AiIngestResponse;
import com.baton.ai.exception.AiClientException;
import com.baton.ai.exception.AiErrorCode;
import com.baton.ai.service.AiClient;
import com.baton.common.BusinessException;
import com.baton.document.dto.DocumentUploadRequest;
import com.baton.document.dto.DocumentUploadResponse;
import com.baton.document.entity.Document;
import com.baton.document.entity.DocumentStatus;
import com.baton.document.entity.FileType;
import com.baton.document.exception.DocumentErrorCode;
import com.baton.document.repository.DocumentRepository;
import com.baton.document.service.FileValidator.ValidatedFile;
import com.baton.member.entity.Department;
import com.baton.member.entity.Member;
import com.baton.member.entity.Rank;
import com.baton.security.AuthMember;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

class DocumentUploadServiceTest {

    private static final AuthMember SALES_STAFF = new AuthMember(1L, "uuid-1", Department.SALES, Rank.STAFF);
    private static final MockMultipartFile FILE = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[]{1});

    private FileValidator fileValidator;
    private DocumentRepository repository;
    private AiClient aiClient;
    private DocumentUploadService service;
    private Document stored;

    @BeforeEach
    void setUp() {
        fileValidator = mock(FileValidator.class);
        repository = mock(DocumentRepository.class);
        aiClient = mock(AiClient.class);
        EntityManager em = mock(EntityManager.class);
        Member ownerRef = mock(Member.class);
        when(em.getReference(Member.class, 1L)).thenReturn(ownerRef);
        // TransactionTemplate은 mock 트랜잭션 매니저로도 콜백을 그대로 실행한다
        service = new DocumentUploadService(fileValidator, new AccessPolicy(), repository, aiClient, em,
                mock(PlatformTransactionManager.class));

        when(fileValidator.validate(any())).thenReturn(
                new ValidatedFile("거래처 관리 매뉴얼.pdf", "거래처 관리 매뉴얼", FileType.PDF, 1L, new byte[]{1}));
        when(repository.save(any(Document.class))).thenAnswer(inv -> {
            stored = inv.getArgument(0);
            ReflectionTestUtils.setField(stored, "id", 11L);
            return stored;
        });
        when(repository.findById(11L)).thenAnswer(inv -> Optional.ofNullable(stored));
    }

    @Test
    @DisplayName("수집 성공 → READY, chunkCount·maskedCount 저장, 제목 기본값은 파일명")
    void success() {
        when(aiClient.ingest(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(new AiIngestResponse(11L, 12, 2));

        DocumentUploadResponse res = service.upload(SALES_STAFF, FILE,
                new DocumentUploadRequest(Department.SALES, Rank.STAFF, null));

        assertThat(res.status()).isEqualTo(DocumentStatus.READY);
        assertThat(res.chunkCount()).isEqualTo(12);
        assertThat(stored.getStatus()).isEqualTo(DocumentStatus.READY);
        assertThat(stored.getMaskedCount()).isEqualTo(2);
        assertThat(stored.getTitle()).isEqualTo("거래처 관리 매뉴얼");
    }

    @Test
    @DisplayName("수집 실패 → FAILED 저장 + 잔여 청크 정리 호출 + 502 AI_INGEST_FAILED")
    void aiFailure() {
        when(aiClient.ingest(anyLong(), any(), any(), any(), any(), any()))
                .thenThrow(new AiClientException("timeout"));

        assertThatThrownBy(() -> service.upload(SALES_STAFF, FILE,
                new DocumentUploadRequest(Department.SALES, Rank.STAFF, "제목")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(AiErrorCode.AI_INGEST_FAILED);

        assertThat(stored.getStatus()).isEqualTo(DocumentStatus.FAILED);
        verify(aiClient).deleteChunks(11L);
    }

    @Test
    @DisplayName("사원이 팀장 공개(LEADER)로 업로드 → 403, 파일 검증·저장·AI 호출 안 함")
    void staffCannotUploadLeaderScope() {
        assertThatThrownBy(() -> service.upload(SALES_STAFF, FILE,
                new DocumentUploadRequest(Department.SALES, Rank.LEADER, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DocumentErrorCode.DOC_FORBIDDEN_SCOPE);

        verify(fileValidator, never()).validate(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("타 부서(DEV)로 업로드 → 403")
    void otherDepartmentForbidden() {
        assertThatThrownBy(() -> service.upload(SALES_STAFF, FILE,
                new DocumentUploadRequest(Department.DEV, Rank.STAFF, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DocumentErrorCode.DOC_FORBIDDEN_SCOPE);
    }

    @Test
    @DisplayName("제목: 공백만 입력하면 파일명, 앞뒤 공백 제거, 200자 제한")
    void resolveTitle() {
        assertThat(DocumentUploadService.resolveTitle("   ", "기본")).isEqualTo("기본");
        assertThat(DocumentUploadService.resolveTitle("  내 제목 ", "기본")).isEqualTo("내 제목");
        assertThat(DocumentUploadService.resolveTitle("가".repeat(250), "기본")).hasSize(200);
    }
}
