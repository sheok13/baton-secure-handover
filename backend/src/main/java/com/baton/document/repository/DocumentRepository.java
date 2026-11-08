package com.baton.document.repository;

import com.baton.document.entity.Document;
import com.baton.document.entity.DocumentStatus;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 권한 범위 문서함 (DB 설계서 5장). 모든 값은 파라미터 바인딩 → SQL Injection 방지.
     * departments·ranks는 AiFilter.of(내 부서, 내 직급)의 값을 그대로 넘긴다 (Chroma 필터와 같은 규칙).
     * sort는 서비스에서 화이트리스트(createdAt, title) 검사 후 Pageable로 전달한다.
     */
    @Query(value = """
            SELECT d FROM Document d JOIN FETCH d.owner
            WHERE d.status = :status
              AND d.department IN :departments
              AND d.minRank IN :ranks
              AND (:keyword IS NULL OR d.title LIKE CONCAT('%', :keyword, '%'))
            """,
            countQuery = """
            SELECT COUNT(d) FROM Document d
            WHERE d.status = :status
              AND d.department IN :departments
              AND d.minRank IN :ranks
              AND (:keyword IS NULL OR d.title LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Document> searchReadable(@Param("status") DocumentStatus status,
                                  @Param("departments") Collection<Department> departments,
                                  @Param("ranks") Collection<Rank> ranks,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    /** 상세·다운로드용. 작성자 이름·직급을 함께 내려주므로 owner를 한 번에 조회 */
    @EntityGraph(attributePaths = "owner")
    Optional<Document> findWithOwnerById(Long id);
}
