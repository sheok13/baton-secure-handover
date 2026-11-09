package com.baton.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baton.ai.dto.SourceDto;
import com.baton.document.entity.Document;
import com.baton.document.entity.FileType;
import com.baton.document.repository.DocumentRepository;
import com.baton.document.service.AccessPolicy;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import com.baton.security.AuthMember;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SourceRevalidatorTest {

    private static final AuthMember SALES_STAFF = new AuthMember(1L, "u1", Department.SALES, Rank.STAFF);
    private static final AuthMember SALES_LEAD = new AuthMember(2L, "u2", Department.SALES, Rank.LEADER);

    private SourceRevalidator revalidator;

    static Document doc(long id, String title, Department dept, Rank rank) {
        Document d = Document.create(null, title, title + ".docx", FileType.DOCX, 1L, dept, rank);
        d.markReady(1, 0);
        ReflectionTestUtils.setField(d, "id", id);
        return d;
    }

    @BeforeEach
    void setUp() {
        DocumentRepository repo = mock(DocumentRepository.class);
        when(repo.findAllById(any())).thenReturn(List.of(
                doc(3, "거래처 관리 매뉴얼", Department.SALES, Rank.STAFF),
                doc(5, "거래처 계약 단가표", Department.SALES, Rank.LEADER),
                doc(7, "개발 환경 세팅 가이드", Department.DEV, Rank.STAFF)));
        revalidator = new SourceRevalidator(repo, new AccessPolicy());
    }

    private static List<SourceDto> fromAi() {
        return List.of(
                new SourceDto(3L, "AI가 붙인 제목", null),
                new SourceDto(5L, "거래처 계약 단가표", null),
                new SourceDto(7L, "개발 환경 세팅 가이드", null),
                new SourceDto(3L, "중복", null),
                new SourceDto(999L, "없는 문서", 1));
    }

    @Test
    @DisplayName("영업 사원: 권한 밖(5 팀장 공개, 7 개발)·없는 문서·중복 제거 → 3번만, 제목은 DB 값")
    void salesStaff() {
        assertThat(revalidator.filter(SALES_STAFF, fromAi()))
                .containsExactly(new SourceDto(3L, "거래처 관리 매뉴얼", null));
    }

    @Test
    @DisplayName("영업 팀장: 3·5번 통과, 7번(개발) 제거")
    void salesLeader() {
        assertThat(revalidator.filter(SALES_LEAD, fromAi()))
                .extracting(SourceDto::docId).containsExactly(3L, 5L);
    }

    @Test
    @DisplayName("출처가 없으면 빈 목록")
    void empty() {
        assertThat(revalidator.filter(SALES_STAFF, List.of())).isEmpty();
        assertThat(revalidator.filter(SALES_STAFF, null)).isEmpty();
    }
}
