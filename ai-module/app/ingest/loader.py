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