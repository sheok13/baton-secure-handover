"""업로드 파일에서 텍스트 추출 (D 소유).

PDF는 페이지마다 Document 1개(page는 1부터), DOCX는 Document 1개(page 없음).
원본 파일은 임시 파일로만 잠깐 쓰고 바로 지운다 (원본 미보관, 아키텍처 5.1).
"""
import os
import tempfile
from pathlib import Path

from langchain_community.document_loaders import Docx2txtLoader, PyPDFLoader
from langchain_core.documents import Document

from app.core.constants import META_PAGE

SUPPORTED_SUFFIXES = (".pdf", ".docx")


class UnsupportedFileTypeError(ValueError):
    """PDF·DOCX가 아닌 파일 → 라우터에서 400으로 응답"""


class EmptyTextError(ValueError):
    """글자를 하나도 못 꺼냄(스캔 PDF 등) → 라우터에서 422 INGEST_EMPTY_TEXT"""

def load_document(data: bytes, filename: str) -> list[Document]:
    suffix = Path(filename).suffix.lower()
    if suffix not in SUPPORTED_SUFFIXES:
        raise UnsupportedFileTypeError(suffix)

    # 로더가 '파일 경로'를 받기 때문에 잠깐 임시 파일로 쓴다
    fd, tmp_path = tempfile.mkstemp(suffix=suffix)
    try:
        with os.fdopen(fd, "wb") as f:
            f.write(data)

        if suffix == ".pdf":
            raw_docs = PyPDFLoader(tmp_path).load()
        else:
            raw_docs = Docx2txtLoader(tmp_path).load()
    finally:
        os.remove(tmp_path)  # 성공하든 실패하든 원본은 남기지 않는다

    docs: list[Document] = []
    for d in raw_docs:
        text = d.page_content.strip()
        if not text:
            continue  # 빈 페이지는 건너뜀

        metadata = {}
        if suffix == ".pdf":
            metadata[META_PAGE] = int(d.metadata.get("page", 0)) + 1  # 0부터 → 1부터
        docs.append(Document(page_content=text, metadata=metadata))