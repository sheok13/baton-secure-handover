"""Keep reference labels out of answer text; preserve the substantive answer."""
import re


def strip_source_labels(text: str, titles: list[str]) -> str:
    # Only remove explicit citation syntax, not ordinary mentions of a topic.
    text = re.sub(r'\((?:출처|참고\s*문서)\s*[:：][^()\n]*\)', '', text)
    text = re.sub(r'\[(?:출처|참고\s*문서)\s*[:：][^\[\]\n]*\]', '', text)
    for title in titles:
        label = re.escape(title)
        text = re.sub(r'\[' + label + r'(?:\s+p\.\s*\d+)?\]', '', text)
        text = re.sub(r'(?m)^\s*(?:출처|참고\s*문서)\s*[:：]\s*' + label
                      + r'(?:\s+p\.\s*\d+)?\s*$', '', text)
    return text
