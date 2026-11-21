package com.baton.document.entity;

import com.baton.common.BaseTimeEntity;
import com.baton.member.entity.Department;
import com.baton.member.entity.Member;
import com.baton.member.entity.Rank;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 문서 메타데이터 (DB 설계서 3.2). 원본 파일은 보관하지 않는다.
 * documents 테이블에는 updated_at이 없으므로 updatedAt 필드를 두지 않는다 (validate 실패 방지).
 */
@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    // @JdbcTypeCode(VARCHAR): Hibernate 7이 MariaDB ENUM 타입을 기대하지 않도록 (V1은 VARCHAR + CHECK)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "file_type", nullable = false, length = 10)
    private FileType fileType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Department department;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "min_rank", nullable = false, length = 10)
    private Rank minRank;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 12)
    private DocumentStatus status;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "masked_count", nullable = false)
    private int maskedCount;

    /** 업로드 시작: PROCESSING으로 저장해 docId를 먼저 받은 뒤 AI 수집을 호출한다. */
    public static Document create(Member owner, String title, String originalFilename,
                                  FileType fileType, long fileSize,
                                  Department department, Rank minRank) {
        Document d = new Document();
        d.owner = owner;
        d.title = title;
        d.originalFilename = originalFilename;
        d.fileType = fileType;
        d.fileSize = fileSize;
        d.department = department;
        d.minRank = minRank;
        d.status = DocumentStatus.PROCESSING;
        d.chunkCount = 0;
        d.maskedCount = 0;
        return d;
    }

    /** AI 수집 성공 */
    public void markReady(int chunkCount, int maskedCount) {
        this.status = DocumentStatus.READY;
        this.chunkCount = chunkCount;
        this.maskedCount = maskedCount;
    }

    /** AI 수집 실패·타임아웃 */
    public void markFailed() {
        this.status = DocumentStatus.FAILED;
    }

    public boolean isReady() {
        return status == DocumentStatus.READY;
    }
}
