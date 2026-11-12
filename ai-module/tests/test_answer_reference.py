from langchain_core.documents import Document

from app.rag.answer_format import strip_source_labels
from app.rag.retriever import select_reference_document, format_sources


def test_only_best_document_chunks_are_used_and_one_source_returned():
    other = Document(page_content='다른 문서', metadata={'doc_id': 2, 'title': '배포 절차서'})
    first = Document(page_content='설치', metadata={'doc_id': 1, 'title': '환경 가이드', 'page': 1})
    second = Document(page_content='실행', metadata={'doc_id': 1, 'title': '환경 가이드', 'page': 2})
    docs = select_reference_document([(other, .7), (first, .95), (second, .8)])
    assert docs == [first, second]
    assert format_sources(docs) == [{'docId': 1, 'title': '환경 가이드', 'page': 1}]
    assert select_reference_document([]) == []


def test_citation_removal_preserves_answer_content():
    text = 'Docker를 설치합니다. (출처: 환경 가이드)\n[환경 가이드]\n참고 문서: 환경 가이드'
    assert strip_source_labels(text, ['환경 가이드']).strip() == 'Docker를 설치합니다.'
    assert strip_source_labels('환경 가이드의 설정값은 [선택]입니다. (WSL2 사용)', ['환경 가이드']) == '환경 가이드의 설정값은 [선택]입니다. (WSL2 사용)'
