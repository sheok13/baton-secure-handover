package com.baton.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baton.common.BusinessException;
import com.baton.document.entity.FileType;
import com.baton.document.exception.DocumentErrorCode;
import com.baton.document.service.FileValidator.ValidatedFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** 11/10 업로드 우회 점검(이중 확장자·MIME 위조·매직넘버 불일치·10MB·경로 문자)과 같은 케이스 */
class FileValidatorTest {

    private static final String PDF = "application/pdf";
    private static final String DOCX = FileType.DOCX.mimeType();

    private final FileValidator validator = new FileValidator();

    private static byte[] pdfBytes() {
        return "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }

    /** 실제 DOCX처럼 첫 항목이 [Content_Types].xml인 zip */
    private static byte[] docxBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static MockMultipartFile file(String name, String mime, byte[] content) {
        return new MockMultipartFile("file", name, mime, content);
    }

    private static void assertRejected(Runnable r, DocumentErrorCode code) {
        assertThatThrownBy(r::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(code);
    }

    @Test
    @DisplayName("정상 PDF 통과, 제목 기본값은 확장자 뺀 파일명")
    void validPdf() {
        ValidatedFile vf = validator.validate(file("거래처 관리 매뉴얼.pdf", PDF, pdfBytes()));
        assertThat(vf.type()).isEqualTo(FileType.PDF);
        assertThat(vf.baseName()).isEqualTo("거래처 관리 매뉴얼");
    }

    @Test
    @DisplayName("정상 DOCX 통과 (OOXML 시그니처)")
    void validDocx() throws IOException {
        ValidatedFile vf = validator.validate(file("배포_절차서.docx", DOCX, docxBytes()));
        assertThat(vf.type()).isEqualTo(FileType.DOCX);
    }

    @Test
    @DisplayName("Content-Type이 octet-stream이어도 매직넘버가 맞으면 통과")
    void genericMimeAllowed() throws IOException {
        assertThat(validator.validate(file("a.docx", "application/octet-stream", docxBytes())).type())
                .isEqualTo(FileType.DOCX);
    }

    @Test
    @DisplayName("이중 확장자 a.pdf.exe → 거부")
    void doubleExtension() {
        assertRejected(() -> validator.validate(file("a.pdf.exe", PDF, pdfBytes())), DocumentErrorCode.FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("MIME 위조 (PDF인데 image/png) → 거부")
    void mimeMismatch() {
        assertRejected(() -> validator.validate(file("a.pdf", "image/png", pdfBytes())), DocumentErrorCode.FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("매직넘버 불일치: 실행 파일을 .pdf로 위장 → 거부")
    void exeDisguisedAsPdf() {
        byte[] exe = "MZ\u0090\u0000fake-executable".getBytes(StandardCharsets.ISO_8859_1);
        assertRejected(() -> validator.validate(file("report.pdf", PDF, exe)), DocumentErrorCode.FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("매직넘버 불일치: 텍스트를 .pdf로 위장 → 거부")
    void textDisguisedAsPdf() {
        assertRejected(() -> validator.validate(file("memo.pdf", PDF, "그냥 텍스트".getBytes(StandardCharsets.UTF_8))),
                DocumentErrorCode.FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("매직넘버 불일치: PDF를 .docx로 위장 → 거부")
    void pdfDisguisedAsDocx() {
        assertRejected(() -> validator.validate(file("a.docx", DOCX, pdfBytes())), DocumentErrorCode.FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("빈 파일 → 거부")
    void emptyFile() {
        assertRejected(() -> validator.validate(file("a.pdf", PDF, new byte[0])), DocumentErrorCode.FILE_INVALID_TYPE);
    }

    @Test
    @DisplayName("10MB 초과 → 413 FILE_TOO_LARGE")
    void tooLarge() {
        byte[] big = new byte[(int) FileValidator.MAX_SIZE_BYTES + 1];
        assertRejected(() -> validator.validate(file("big.pdf", PDF, big)), DocumentErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("파일명 정제: 경로·상위 폴더·스크립트 문자 제거")
    void sanitizeFilename() {
        assertThat(FileValidator.sanitizeFilename("../../etc/passwd.pdf")).isEqualTo("passwd.pdf");
        assertThat(FileValidator.sanitizeFilename("C:\\Users\\me\\보고서.docx")).isEqualTo("보고서.docx");
        // "</script>"의 "/"가 경로 구분자로 처리되어 마지막 조각만 남고, < > 는 제거된다
        assertThat(FileValidator.sanitizeFilename("<script>alert(1)</script>.pdf")).isEqualTo("script.pdf");
        assertThat(FileValidator.sanitizeFilename("<b>보고서</b>.pdf".replace("/", ""))).isEqualTo("b보고서b.pdf");
        assertThat(FileValidator.sanitizeFilename("a\u0000b.pdf")).isEqualTo("ab.pdf");
    }

    @Test
    @DisplayName("255자 넘는 파일명은 확장자를 남기고 자른다")
    void longFilenameTruncated() {
        String name = FileValidator.sanitizeFilename("가".repeat(300) + ".pdf");
        assertThat(name).hasSize(255).endsWith(".pdf");
    }
}
