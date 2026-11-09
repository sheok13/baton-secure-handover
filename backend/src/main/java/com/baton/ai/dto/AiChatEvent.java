package com.baton.ai.dto;

/** AI SSE 이벤트 한 건. event = token | sources | done | error, data = JSON 문자열 */
public record AiChatEvent(String event, String data) {

    public static final String TOKEN = "token";
    public static final String SOURCES = "sources";
    public static final String DONE = "done";
    public static final String ERROR = "error";
}
