package com.baton.chat.service;

import com.baton.ai.dto.SourceDto;
import com.baton.document.entity.Document;
import com.baton.document.repository.DocumentRepository;
import com.baton.document.service.AccessPolicy;
import com.baton.security.AuthMember;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI가 보낸 출처를 DB 권한으로 다시 검사 (아키텍처 Defense in Depth 마지막 단계).
 * Chroma 필터가 잘못돼 권한 밖 청크가 섞여도 사용자에게는 보이지 않고 저장되지도 않는다.
 * 걸러진 출처는 유출 시도·필터 오류의 신호이므로 감사 로그로 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SourceRevalidator {

    private final DocumentRepository documentRepository;
    private final AccessPolicy accessPolicy;

    public List<SourceDto> filter(AuthMember me, List<SourceDto> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = sources.stream().map(SourceDto::docId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Document> docs = documentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Document::getId, Function.identity()));

        List<SourceDto> allowed = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (SourceDto s : sources) {
            Document doc = s.docId() == null ? null : docs.get(s.docId());
            if (doc == null || !accessPolicy.canRead(me.department(), me.rank(), doc)) {
                // TODO(A): AuditLogger 머지 후 감사 로그로 교체
                log.warn("[AUDIT] SOURCE_FILTERED memberUuid={} docId={}", me.uuid(), s.docId());
                continue;
            }
            if (seen.add(s.docId() + ":" + s.page())) {
                // 제목은 AI 값 대신 DB 값을 쓴다
                allowed.add(new SourceDto(doc.getId(), doc.getTitle(), s.page()));
            }
        }
        return allowed;
    }
}
