import asyncio
from types import SimpleNamespace

import pytest
from langchain_core.documents import Document
from langchain_core.runnables import RunnableLambda

from app.rag.answer_format import SourceLabelStream
from app.rag import chain


def render(parts):
    formatter = SourceLabelStream(['환경 가이드'])
    return ''.join(formatter.feed(part) for part in parts) + formatter.finish()


def test_single_line_is_sent_before_generation_finishes():
    formatter = SourceLabelStream(['환경 가이드'])
    assert formatter.feed('여름휴가는 ') == '여름휴가는 '
    assert formatter.feed('3일입니다.') == '3일입니다.'
    assert formatter.finish() == ''


@pytest.mark.parametrize('text', [
    '첫 문단입니다.\n\n두 번째 문단입니다.\n',
    '1. 설치\n\n2. 실행\n   - 확인\n\n',
    '\n\n시작\n\n',
    '일반 [선택] 및 (WSL2 사용) 문장은 유지합니다.',
])
def test_markdown_and_non_citation_brackets_preserved(text):
    assert render(list(text)) == text


@pytest.mark.parametrize('label', ['[환경 가이드]', '[환경 가이드 p.2]',
                                  '(출처: 환경 가이드)', '[참고 문서: 환경 가이드]',
                                  '참고 문서: 환경 가이드'])
def test_reference_line_removed_at_every_token_boundary(label):
    for offset in range(1, len(label)):
        assert render(['내용\n', label[:offset], label[offset:], '\n', '\n다음 문단']) == '내용\n\n다음 문단'


def test_ordinary_long_paragraph_is_not_buffered():
    formatter = SourceLabelStream([])
    for _ in range(1000):
        assert formatter.feed('긴 답변입니다. ') == '긴 답변입니다. '


def test_unclosed_candidate_is_bounded_and_flushed():
    formatter = SourceLabelStream(['환경 가이드'])
    assert formatter.feed('(출처:') == ''
    assert formatter.feed('x' * 600)
    assert render(['일반 (WSL2']) == '일반 (WSL2'


def test_chain_yields_before_model_next_token(monkeypatch):
    async def scenario():
        release = asyncio.Event()

        async def model(_):
            yield '여름휴가는 '
            await release.wait()
            yield '3일입니다.\n\n1. 신청\n2. 승인'

        monkeypatch.setattr(chain, 'get_llm', lambda: RunnableLambda(model))
        monkeypatch.setattr(chain, 'get_settings', lambda: SimpleNamespace(
            max_question_chars=1000, max_history_messages=6))
        answer = chain.stream_answer('휴가 안내', [Document(
            page_content='휴가 3일', metadata={'title': '환경 가이드'})], [])
        try:
            first = await asyncio.wait_for(anext(answer), timeout=1)
            assert first == '여름휴가는 '
            release.set()
            rest = ''.join([part async for part in answer])
            assert first + rest == '여름휴가는 3일입니다.\n\n1. 신청\n2. 승인'
        finally:
            release.set()
            await answer.aclose()

    asyncio.run(scenario())
