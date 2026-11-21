package com.baton.document.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** 업로드 허용 형식. 확장자·MIME 값은 FileValidator(3단계 검증)에서 사용한다. */
public enum FileType {

    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final String extension;
    private final String mimeType;

    FileType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String extension() {
        return extension;
    }

    public String mimeType() {
        return mimeType;
    }

    /** "pdf", "PDF" → PDF. 허용 목록에 없으면 empty. */
    public static Optional<FileType> fromExtension(String ext) {
        if (ext == null) {
            return Optional.empty();
        }
        String lower = ext.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(t -> t.extension.equals(lower)).findFirst();
    }
}
