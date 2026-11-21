package com.baton.document.dto;

import com.baton.document.entity.Document;
import com.baton.document.entity.DocumentStatus;
import com.baton.member.entity.Department;
import com.baton.member.entity.Rank;
import java.util.List;
import org.springframework.data.domain.Page;

/** GET /documents 응답 (API 설계서 2장 페이징 형식 + 4.1). mine 필드는 v0.3에서 제거 */
public record DocumentListResponse(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public record Item(
            Long id,
            String title,
            AuthorResponse author,
            String createdAt,
            Department department,
            Rank minRank,
            DocumentStatus status
    ) {
        public static Item from(Document d) {
            return new Item(
                    d.getId(),
                    d.getTitle(),
                    AuthorResponse.from(d.getOwner()),
                    KstTime.format(d.getCreatedAt()),
                    d.getDepartment(),
                    d.getMinRank(),
                    d.getStatus()
            );
        }
    }

    public static DocumentListResponse from(Page<Document> page) {
        return new DocumentListResponse(
                page.getContent().stream().map(Item::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
