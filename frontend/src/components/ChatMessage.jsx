import ReactMarkdown from 'react-markdown';
import SourceChip from './SourceChip';

const markdownComponents = {
  p: ({ children }) => <p className="my-2 whitespace-pre-wrap">{children}</p>,
  ul: ({ children }) => <ul className="my-2 list-disc pl-6">{children}</ul>,
  ol: ({ children }) => <ol className="my-2 list-decimal pl-6">{children}</ol>,
  li: ({ children }) => <li className="my-1">{children}</li>,
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
            <ReactMarkdown components={markdownComponents}>
              {message.content ?? ''}
            </ReactMarkdown>
          )}

          {message.error && (
            <div role="alert" className="mt-2 text-sm text-danger">
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

        {!isUser && message.sources?.length > 0 && (
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-[13px] text-sub">참고 문서</span>
            <SourceChip source={message.sources[0]} />
          </div>
        )}
      </div>
    </div>
  );
}
