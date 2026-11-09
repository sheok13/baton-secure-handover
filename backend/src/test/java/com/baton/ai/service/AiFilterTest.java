package com.baton.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiFilterTest {

    @Test
    @DisplayName("영업 사원: 부서 [SALES, ALL] / 직급 [STAFF]")
    void salesStaff() {
        AiFilter f = AiFilter.of(Department.SALES, Rank.STAFF);
        assertThat(f.departments()).containsExactly(Department.SALES, Department.ALL);
        assertThat(f.ranks()).containsExactly(Rank.STAFF);
    }

    @Test
    @DisplayName("개발 팀장: 부서 [DEV, ALL] / 직급 [STAFF, LEADER]")
    void devLeader() {
        AiFilter f = AiFilter.of(Department.DEV, Rank.LEADER);
        assertThat(f.departments()).containsExactly(Department.DEV, Department.ALL);
        assertThat(f.ranks()).containsExactly(Rank.STAFF, Rank.LEADER);
    }

    @Test
    @DisplayName("회원 부서가 ALL이면 거부 (fail-closed)")
    void memberDepartmentAllIsRejected() {
        assertThatThrownBy(() -> AiFilter.of(Department.ALL, Rank.LEADER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("필터 리스트는 외부에서 수정할 수 없다")
    void immutable() {
        AiFilter f = AiFilter.of(Department.SALES, Rank.STAFF);
        assertThatThrownBy(() -> f.ranks().add(Rank.LEADER))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
