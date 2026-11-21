package com.baton.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baton.common.BusinessException;
import com.baton.document.dto.DocumentDetailResponse;
import com.baton.document.entity.Document;
import com.baton.document.entity.DocumentStatus;
import com.baton.document.entity.FileType;
import com.baton.document.exception.DocumentErrorCode;
import com.baton.document.repository.DocumentRepository;
import com.baton.member.entity.Department;
import com.baton.member.entity.Member;
import com.baton.member.entity.Rank;
import com.baton.security.AuthMember;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class DocumentServiceTest {

    private static final AuthMember SALES_STAFF = new AuthMember(1L, "uuid-sales-staff", Department.SALES, Rank.STAFF);

    private DocumentRepository repository;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentRepository.class);
        service = new DocumentService(repository, new AccessPolicy());
    }

    private static Document readyDoc(Department dept, Rank rank) {
        Member owner = mock(Member.class);
        when(owner.getName()).thenReturn("김영업");
        when(owner.getJobRank()).thenReturn(Rank.STAFF);
        Document d = Document.create(owner, "거래처 관리 매뉴얼", "거래처_관리_매뉴얼.pdf",
                FileType.PDF, 524288L, dept, rank);
        d.markReady(12, 2);
        return d;
    }

    @Test
    @DisplayName("열람 가능한 문서 상세 → 작성자 이름·직급 포함")
    void detailReadable() {
        Document doc = readyDoc(Department.SALES, Rank.STAFF);   // when() 안에서 mock을 만들면 Mockito 오류 → 먼저 생성
        when(repository.findWithOwnerById(3L)).thenReturn(Optional.of(doc));

        DocumentDetailResponse res = service.getDetail(SALES_STAFF, 3L);

        assertThat(res.title()).isEqualTo("거래처 관리 매뉴얼");
        assertThat(res.author().name()).isEqualTo("김영업");
        assertThat(res.maskedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("권한 밖 문서(영업 사원 → 영업 팀장 공개) → 404 DOC_NOT_FOUND")
    void detailForbiddenIs404() {
        Document doc = readyDoc(Department.SALES, Rank.LEADER);
        when(repository.findWithOwnerById(5L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.getDetail(SALES_STAFF, 5L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DocumentErrorCode.DOC_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 문서 → 권한 밖과 같은 404 (존재 여부 숨김)")
    void detailMissingIs404() {
        when(repository.findWithOwnerById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(SALES_STAFF, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DocumentErrorCode.DOC_NOT_FOUND);
    }

    @Test
    @DisplayName("타 부서 문서 다운로드 시도(IDOR) → 404")
    void downloadOtherDepartmentIs404() {
        Document doc = readyDoc(Department.DEV, Rank.STAFF);
        when(repository.findWithOwnerById(7L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.download(SALES_STAFF, 7L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("sort 화이트리스트 밖 필드(password) → 400")
    void invalidSortRejected() {
        Pageable p = PageRequest.of(0, 10, Sort.by("password"));

        assertThatThrownBy(() -> service.search(SALES_STAFF, null, p))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DocumentErrorCode.INVALID_SORT);
    }

    @Test
    @DisplayName("목록 조회는 DB 회원 기준 필터 [SALES, ALL] / [STAFF]로만 검색하고, 페이지 크기는 50으로 제한")
    void searchUsesMemberFilter() {
        Page<Document> empty = new PageImpl<>(List.of());
        when(repository.searchReadable(any(), anyCollection(), anyCollection(), any(), any())).thenReturn(empty);

        service.search(SALES_STAFF, "  ", PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt")));

        verify(repository).searchReadable(
                eq(DocumentStatus.READY),
                eq(List.of(Department.SALES, Department.ALL)),
                eq(List.of(Rank.STAFF)),
                isNull(),
                eq(PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @Test
    @DisplayName("검색어의 % _ \\ 는 글자 그대로 검색되도록 이스케이프")
    void keywordEscaped() {
        assertThat(service.normalizeKeyword(" 50%_할인 ")).isEqualTo("50\\%\\_할인");
        assertThat(service.normalizeKeyword("a\\b")).isEqualTo("a\\\\b");
    }

    @Test
    @DisplayName("검색어 100자 초과 → 400")
    void keywordTooLong() {
        assertThatThrownBy(() -> service.normalizeKeyword("가".repeat(101)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(DocumentErrorCode.INVALID_KEYWORD);
    }
}
