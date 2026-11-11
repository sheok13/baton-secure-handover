package com.baton.ai.service;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** SSE 이벤트(data 필드) 직렬화·역직렬화 전용 Jackson 3 매퍼 */
public final class AiJson {

    public static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private AiJson() {
    }
}