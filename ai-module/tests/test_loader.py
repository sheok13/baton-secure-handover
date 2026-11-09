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