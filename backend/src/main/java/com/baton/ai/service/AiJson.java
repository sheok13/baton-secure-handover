package com.baton.ai.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SSE 이벤트(data 필드) 직렬화·역직렬화 전용 Jackson ObjectMapper
 */
public final class AiJson {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private AiJson() {
    }
}
