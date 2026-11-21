package com.baton.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.baton.document.entity.Document;
import com.baton.document.entity.FileType;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 계정 4개 × 권한 그룹 6개 (DB 설계서 6장 시드 문서 5그룹 + ALL·LEADER) */
class AccessPolicyTest {

    private final AccessPolicy policy = new AccessPolicy();

    private static Document readyDoc(Department dept, Rank minRank) {
        Document d = Document.create(null, "테스트 문서", "test.pdf", FileType.PDF, 1024L, dept, minRank);
        d.markReady(3, 0);
        return d;
    }

    @ParameterizedTest(name = "{0}·{1} 회원 → {2}·{3} 문서 열람 = {4}")
    @CsvSource({
            // 영업 사원
            "SALES, STAFF,  ALL,   STAFF,  true",
            "SALES, STAFF,  ALL,   LEADER, false",
            "SALES, STAFF,  SALES, STAFF,  true",
            "SALES, STAFF,  SALES, LEADER, false",
            "SALES, STAFF,  DEV,   STAFF,  false",
            "SALES, STAFF,  DEV,   LEADER, false",
            // 영업 팀장
            "SALES, LEADER, ALL,   STAFF,  true",
            "SALES, LEADER, ALL,   LEADER, true",
            "SALES, LEADER, SALES, STAFF,  true",
            "SALES, LEADER, SALES, LEADER, true",
            "SALES, LEADER, DEV,   STAFF,  false",
            "SALES, LEADER, DEV,   LEADER, false",
            // 개발 사원
            "DEV,   STAFF,  ALL,   STAFF,  true",
            "DEV,   STAFF,  ALL,   LEADER, false",
            "DEV,   STAFF,  SALES, STAFF,  false",
            "DEV,   STAFF,  SALES, LEADER, false",
            "DEV,   STAFF,  DEV,   STAFF,  true",
            "DEV,   STAFF,  DEV,   LEADER, false",
            // 개발 팀장
            "DEV,   LEADER, ALL,   STAFF,  true",
            "DEV,   LEADER, ALL,   LEADER, true",
            "DEV,   LEADER, SALES, STAFF,  false",
            "DEV,   LEADER, SALES, LEADER, false",
            "DEV,   LEADER, DEV,   STAFF,  true",
            "DEV,   LEADER, DEV,   LEADER, true",
    })
    void canRead(Department myDept, Rank myRank, Department docDept, Rank docRank, boolean expected) {
        assertThat(policy.canRead(myDept, myRank, readyDoc(docDept, docRank))).isEqualTo(expected);
    }

    @Test
    @DisplayName("READY가 아닌 문서(PROCESSING·FAILED)는 권한이 있어도 열람 불가")
    void notReadyIsHidden() {
        Document processing = Document.create(null, "처리중", "a.pdf", FileType.PDF, 1L, Department.ALL, Rank.STAFF);
        Document failed = Document.create(null, "실패", "b.pdf", FileType.PDF, 1L, Department.ALL, Rank.STAFF);
        failed.markFailed();

        assertThat(policy.canRead(Department.SALES, Rank.LEADER, processing)).isFalse();
        assertThat(policy.canRead(Department.SALES, Rank.LEADER, failed)).isFalse();
    }

    @ParameterizedTest(name = "{0}·{1} 회원 → {2}·{3} 범위 업로드 = {4}")
    @CsvSource({
            "SALES, STAFF,  SALES, STAFF,  true",
            "SALES, STAFF,  ALL,   STAFF,  true",
            "SALES, STAFF,  SALES, LEADER, false",   // 사원은 팀장 공개 지정 불가
            "SALES, STAFF,  DEV,   STAFF,  false",   // 타 부서 지정 불가
            "SALES, LEADER, SALES, LEADER, true",
            "SALES, LEADER, ALL,   LEADER, true",
            "SALES, LEADER, DEV,   LEADER, false",
            "DEV,   STAFF,  DEV,   STAFF,  true",
            "DEV,   LEADER, SALES, STAFF,  false",
    })
    void canUpload(Department myDept, Rank myRank, Department target, Rank targetRank, boolean expected) {
        assertThat(policy.canUpload(myDept, myRank, target, targetRank)).isEqualTo(expected);
    }
}
