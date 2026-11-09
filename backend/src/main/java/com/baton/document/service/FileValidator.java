package com.baton.document.service;

import com.baton.common.BusinessException;
import com.baton.document.entity.FileType;
import com.baton.document.exception.DocumentErrorCode;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 파일 3단계 검증 (Backend 명세 7장, API 설계서 4.2).
 * 1) 파일명 정제 + 확장자 화이트리스트(pdf, docx)  2) Content-Type 확인  3) Tika 매직넘버로 실제 형식 확인
 * 크기는 multipart 설정(10MB)이 먼저 막고, 여기서 한 번 더 확인한다.
 */
@Component
public class FileValidator {

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_FILENAME_LENGTH = 255;

    /** 브라우저·OS에 따라 DOCX MIME을 모르면 비우거나 octet-stream으로 보낸다 → 허용하고 매직넘버로 판단 */
    private static final Set<String> GENERIC_MIME = Set.of("", "application/octet-stream");

    private static final String PDF_MAGIC = "application/pdf";
    /** tika-core가 OOXML(zip + [Content_Types].xml) 시그니처로 판정하는 타입 */
    private static final Set<String> DOCX_MAGIC = Set.of(
            "application/x-tika-ooxml",
            FileType.DOCX.mimeType());

    private final Tika tika = new Tika();

    public ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(DocumentErrorCode.FILE_INVALID_TYPE);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException(DocumentErrorCode.FILE_TOO_LARGE);
        }

        // 1) 파일명 정제 + 확장자 (이중 확장자 a.pdf.exe → 마지막 확장자 exe → 거부)
        String filename = sanitizeFilename(file.getOriginalFilename());
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) {
            throw new BusinessException(DocumentErrorCode.FILE_INVALID_TYPE);
        }
        FileType type = FileType.fromExtension(filename.substring(dot + 1))
                .orElseThrow(() -> new BusinessException(DocumentErrorCode.FILE_INVALID_TYPE));

        // 2) 요청 Content-Type (위조 가능하므로 보조 검사)
        String declared = normalizeMime(file.getContentType());
        if (!declared.equals(type.mimeType()) && !GENERIC_MIME.contains(declared)) {
            throw new BusinessException(DocumentErrorCode.FILE_INVALID_TYPE);
        }

        // 3) 매직넘버 — 파일명 힌트 없이 내용만으로 판정 (확장자만 바꾼 파일 차단)
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(DocumentErrorCode.FILE_INVALID_TYPE);
        }
        String detected = tika.detect(content);
        boolean magicOk = switch (type) {
            case PDF -> PDF_MAGIC.equals(detected);
            case DOCX -> DOCX_MAGIC.contains(detected);
        };
        if (!magicOk) {
            throw new BusinessException(DocumentErrorCode.FILE_INVALID_TYPE);
        }

        return new ValidatedFile(filename, filename.substring(0, dot), type, content.length, content);
    }

    /**
     * 경로 제거(../, 폴더), 제어문자·Windows 금지문자(< > : " | ? *) 제거, 255자 제한(확장자 유지).
     * 화면 출력 시 이스케이프는 Frontend가 한 번 더 한다.
     */
    static String sanitizeFilename(String original) {
        if (original == null) {
            throw new BusinessException(DocumentErrorCode.FILE_INVALID_TYPE);
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("\\p{Cntrl}", "")
                .replaceAll("[<>:\"|?*]", "")
                .replace("..", "")
                .strip();
        if (name.isEmpty() || name.equals(".")) {
            throw new BusinessException(DocumentErrorCode.FILE_INVALID_TYPE);
        }
        if (name.length() > MAX_FILENAME_LENGTH) {
            int dot = name.lastIndexOf('.');
            String ext = dot > 0 ? name.substring(dot) : "";
            name = name.substring(0, MAX_FILENAME_LENGTH - ext.length()) + ext;
        }
        return name;
    }

    private static String normalizeMime(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semi = contentType.indexOf(';');
        String base = semi >= 0 ? contentType.substring(0, semi) : contentType;
        return base.strip().toLowerCase(Locale.ROOT);
    }

    /** 검증을 통과한 파일. baseName은 제목 기본값(확장자 제외) */
    public record ValidatedFile(String filename, String baseName, FileType type, long size, byte[] content) {
    }
}
