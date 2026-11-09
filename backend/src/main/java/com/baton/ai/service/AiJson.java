package com.baton.ai.service;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI SSE의 data(JSON 문자열) 읽기·쓰기용 매퍼 (Jackson 3, Boot 4 기본).
 * 빈 주입 없이 쓰려고 정적으로 둔다. 모르는 필드가 와도 실패하지 않게 한다.
 */
public final class AiJson {

    public static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private AiJson() {
    }
}
