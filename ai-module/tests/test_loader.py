"""loader·splitter 테스트 (D 소유). 테스트용 PDF·DOCX는 conftest.py가 코드로 만든다."""
import os
 
# 테스트용 필수 환경변수 (실제 키 아님). app import 전에 설정해야 한다.
os.environ.setdefault("AI_INTERNAL_API_KEY", "test-internal-key-0123456789")
os.environ.setdefault("LLM_PROVIDER", "groq")
os.environ.setdefault("LLM_MODEL", "test-model")
os.environ.setdefault("GROQ_API_KEY", "test-groq-key")
os.environ.setdefault("VECTOR_STORE_PATH", "./data/chroma/test")
 
import pytest  # noqa: E402
from langchain_core.documents import Document  # noqa: E402
 
from app.core.constants import META_PAGE  # noqa: E402
from app.ingest.loader import EmptyTextError, UnsupportedFileTypeError, load_document  # noqa: E402
from app.ingest.splitter import split_documents  # noqa: E402

# ── loader ──
 
def test_pdf_pages_start_from_1(make_pdf):
    docs = load_document(make_pdf(["Settlement closes on the 25th.", "Second page"]), "manual.pdf")
    assert [d.metadata[META_PAGE] for d in docs] == [1, 2]  # 0부터가 아니라 1부터 (명세서 5.1)
    assert "25th" in docs[0].page_content
 
 
def test_docx_has_no_page_key(make_docx):
    docs = load_document(make_docx(["거래처 등록 안내", "연락처 010-0000-1234"]), "guide.docx")
    assert len(docs) == 1
    assert META_PAGE not in docs[0].metadata  # Chroma 메타데이터는 null 불가 → 키 자체를 뺀다
    assert "거래처 등록 안내" in docs[0].page_content
 
 
def test_rejects_unsupported_type():
    with pytest.raises(UnsupportedFileTypeError):
        load_document(b"hello", "note.txt")
 
 
def test_rejects_pdf_without_text(make_pdf):
    # 스캔본처럼 글자가 없는 PDF → 422 INGEST_EMPTY_TEXT 의 원인
    with pytest.raises(EmptyTextError):
        load_document(make_pdf([""]), "scan.pdf")


# ── splitter ──
 
def test_split_keeps_size_and_page():
    text = ("거래처별 매출과 수금 내역은 매월 25일까지 ERP에 입력한다. " * 60).strip()
    chunks = split_documents([Document(page_content=text, metadata={META_PAGE: 3})])
    assert len(chunks) >= 2
    assert all(len(c.page_content) <= 800 for c in chunks)  # CHUNK_SIZE
    assert all(c.metadata[META_PAGE] == 3 for c in chunks)  # 조각마다 원래 페이지 유지
 
 
def test_short_doc_is_one_chunk():
    chunks = split_documents([Document(page_content="짧은 문서", metadata={})])
    assert [c.page_content for c in chunks] == ["짧은 문서"]