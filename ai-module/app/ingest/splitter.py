"""텍스트를 청크(조각)로 분할 (D 소유).

청크 800자 / 겹침 150자 (DB 설계서 7장). 페이지 경계를 넘지 않도록
loader가 만든 Document(페이지)마다 따로 자른다.
"""
from functools import lru_cache

from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.core.config import get_settings