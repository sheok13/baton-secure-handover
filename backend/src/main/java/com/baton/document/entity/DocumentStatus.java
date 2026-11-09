package com.baton.document.entity;

/** 문서 수집 상태. 목록·상세·다운로드·채팅 출처는 READY만 노출한다. */
public enum DocumentStatus {
    PROCESSING,
    READY,
    FAILED
}
