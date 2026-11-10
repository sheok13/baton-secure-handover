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
      <div className="max-w-[80%] rounded-lg bg-gray-800 px-3 py-2 text-left">
        {/* 사용자 글은 그대로, AI 답변만 마크다운으로 해석 */}
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
        ) : (
          <ReactMarkdown components={mdComponents}>
            {message.content ?? ''}
          </ReactMarkdown>
        )}

        {message.streaming && (
          <p className="mt-1 text-sm text-gray-400">답변 작성 중…</p>
        )}

        {message.error && (
          <div className="mt-2 text-sm text-red-400">
            <p>{message.error}</p>
            {onRetry && (
              <button
                type="button"
                className="mt-1 rounded border px-2 py-1"
                onClick={onRetry}
              >
                다시 시도
              </button>
            )}
          </div>
        )}

        {message.sources?.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1">
            {message.sources.map((s, i) => (
              <SourceChip key={`${s.docId}-${s.page ?? 'na'}-${i}`} source={s} />
                          ))}
          </div>
        )}
      </div>
    </div>
  );
}