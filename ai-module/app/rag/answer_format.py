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


class SourceLabelStream:
    """Stream ordinary text immediately; buffer only possible reference labels."""

    def __init__(self, titles: list[str]):
        self.titles = titles
        self.pending = ''
        self.mode = None
        self.line_has_text = False
        self.removed_on_line = False

    def _possible_label(self):
        value = self.pending
        if self.mode == 'bracket':
            value = value[1:]
        prefixes = ['출처:', '출처：', '참고 문서:', '참고 문서：']
        if self.mode == 'bracket' and self.pending.startswith('['):
            prefixes += self.titles
        compact = re.sub(r'\s+', '', value)
        return any(re.sub(r'\s+', '', p).startswith(compact)
                   or compact.startswith(re.sub(r'\s+', '', p)) for p in prefixes)

    def _resolve(self):
        raw = self.pending
        self.pending = ''
        self.mode = None
        cleaned = strip_source_labels(raw, self.titles)
        if raw and not cleaned.strip():
            self.removed_on_line = True
            return ''
        self.line_has_text |= bool(cleaned.strip())
        return cleaned

    def feed(self, text: str) -> str:
        out = []
        for char in text:
            if char == '\n':
                if self.pending:
                    out.append(self._resolve())
                # Preserve original empty lines, including Markdown paragraph breaks.
                if self.line_has_text or not self.removed_on_line:
                    out.append('\n')
                self.line_has_text = False
                self.removed_on_line = False
                continue
            if self.pending:
                self.pending += char
                closed = self.mode == 'bracket' and (
                    (self.pending[0] == '[' and char == ']') or
                    (self.pending[0] == '(' and char == ')'))
                # Bounded buffering also handles malformed/unclosed model output.
                if closed or not self._possible_label() or len(self.pending) >= 512:
                    out.append(self._resolve())
                continue
            if char in '[(':
                self.mode = 'bracket'
                self.pending = char
            elif not self.line_has_text and char in '출참':
                self.mode = 'line'
                self.pending = char
            else:
                out.append(char)
                self.line_has_text |= bool(char.strip())
        return ''.join(out)

    def finish(self) -> str:
        return self._resolve() if self.pending else ''
