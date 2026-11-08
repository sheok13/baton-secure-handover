package com.baton.document.service;

import com.baton.ai.service.AiFilter;
import com.baton.document.entity.Document;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import org.springframework.stereotype.Component;

/**
 * 문서 권한 판정 (DB 설계서 5장, Backend 명세 5.1).
 * canRead(member, doc) = (doc.department = ALL OR = 내 부서) AND (doc.minRank = STAFF OR 내 직급 = LEADER) AND status = READY
 */
@Component
public class AccessPolicy {

    /** 열람·다운로드·채팅 출처 재검증 공통. false면 호출 측에서 404 DOC_NOT_FOUND (존재 여부 숨김) */
    public boolean canRead(Department myDepartment, Rank myRank, Document doc) {
        if (doc == null || !doc.isReady()) {
            return false;
        }
        return AiFilter.of(myDepartment, myRank).allows(doc.getDepartment(), doc.getMinRank());
    }

    /** 업로드 범위: 부서는 ALL 또는 내 부서, 사원은 STAFF 공개만. false면 403 DOC_FORBIDDEN_SCOPE */
    public boolean canUpload(Department myDepartment, Rank myRank,
                             Department targetDepartment, Rank targetMinRank) {
        if (targetDepartment == null || targetMinRank == null) {
            return false;
        }
        return AiFilter.of(myDepartment, myRank).allows(targetDepartment, targetMinRank);
    }
}
