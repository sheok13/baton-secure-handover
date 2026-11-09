package com.baton.ai.service;

import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import java.util.List;
import java.util.Objects;

/**
 * 권한 필터 — DB 문서함 조회, AccessPolicy, AI(Chroma where) 세 곳이 같은 규칙을 쓰도록 한 곳에서 계산한다.
 * JSON 직렬화 결과: {"departments":["SALES","ALL"],"ranks":["STAFF"]} (API 설계서 6.2)
 *
 * 입력은 반드시 DB에서 조회한 회원의 부서·직급이어야 한다 (요청 본문 값 사용 금지).
 */
public record AiFilter(List<Department> departments, List<Rank> ranks) {

    public AiFilter {
        departments = List.copyOf(departments);
        ranks = List.copyOf(ranks);
    }

    /** 내 부서 + ALL / 팀장은 STAFF·LEADER, 사원은 STAFF만 */
    public static AiFilter of(Department myDepartment, Rank myRank) {
        Objects.requireNonNull(myDepartment, "department");
        Objects.requireNonNull(myRank, "rank");
        if (myDepartment == Department.ALL) {
            // 회원 부서는 SALES·DEV만 허용 (V1 CHECK). ALL이 들어오면 데이터 오류이므로 fail-closed
            throw new IllegalArgumentException("회원 부서는 ALL일 수 없습니다.");
        }
        List<Rank> ranks = (myRank == Rank.LEADER)
                ? List.of(Rank.STAFF, Rank.LEADER)
                : List.of(Rank.STAFF);
        return new AiFilter(List.of(myDepartment, Department.ALL), ranks);
    }

    /** 문서의 열람 부서·공개 직급이 이 필터 범위 안인지 */
    public boolean allows(Department docDepartment, Rank docMinRank) {
        return departments.contains(docDepartment) && ranks.contains(docMinRank);
    }
}
