package com.baton.document.dto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * API 응답 시각 형식 (API 설계서 예시: "2026-11-09T10:12:00+09:00").
 * DB는 hibernate.jdbc.time_zone=Asia/Seoul 기준 LocalDateTime으로 저장되므로 KST 오프셋을 붙여 문자열로 내려준다.
 * 채팅 응답(3단계)에서도 같이 사용한다.
 */
public final class KstTime {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private KstTime() {
    }

    public static String format(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.truncatedTo(ChronoUnit.SECONDS)
                .atZone(KST)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
