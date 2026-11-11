import ReactMarkdown from 'react-markdown';
import SourceChip from './SourceChip';

// 마크다운이 만드는 요소별 스타일
// 이유: Tailwind v4 preflight가 목록의 점·번호와 여백을 지워서 클래스로 다시 줘야 보입니다.
// 힌트: my-1 / list-disc / list-decimal / pl-5
const mdComponents = {
  p: ({ children }) => <p className="my-1">{children}</p>,
  ul: ({ children }) => <ul className="my-1 list-disc pl-5">{children}</ul>,
  ol: ({ children }) => <ol className="my-1 list-decimal pl-5">{children}</ol>,
};

export default function ChatMessage({ message, onRetry }) {
  const isUser = message.role === 'USER';
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`flex flex-col gap-2 ${isUser ? 'max-w-[70%]' : 'max-w-[80%]'}`}>
        <div
          className={`px-4 py-3 text-left text-[15px] ${
            isUser
              ? 'rounded-[16px_16px_4px_16px] bg-brand leading-relaxed text-white'
              : 'rounded-[16px_16px_16px_4px] bg-bubble leading-[1.7] text-ink'
          }`}
        >
          {/* 사용자 글은 그대로, AI 답변만 마크다운으로 해석 */}
          {isUser ? (
            <p className="whitespace-pre-wrap">{message.content}</p>
          ) : (
            <ReactMarkdown components={mdComponents}>
              {message.content ?? ''}
            </ReactMarkdown>
          )}

          {message.error && (
            <div className="mt-2 text-sm text-danger">
              <p>{message.error}</p>
              {onRetry && (
                <button
                  type="button"
                  className="mt-1 min-h-9 rounded-lg border border-field bg-white px-3 text-ink hover:bg-bg"
                  onClick={onRetry}
                >
                  다시 시도
                </button>
              )}
            </div>
          )}
        </div>

        {message.streaming && <span className="text-[13px] text-sub">답변 작성 중…</span>}

        {message.sources?.length > 0 && (
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-[13px] text-sub">출처</span>
            {message.sources.map((s, i) => (
              <SourceChip key={`${s.docId}-${s.page ?? 'na'}-${i}`} source={s} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
