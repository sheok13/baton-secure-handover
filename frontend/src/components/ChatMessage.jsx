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
      <div className="max-w-[80%] rounded-lg bg-gray-800 px-3 py-2 text-left">
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
        ) : (
          <ReactMarkdown components={markdownComponents}>
            {message.content ?? ''}
          </ReactMarkdown>
        )}
        {message.streaming && <p className="mt-1 text-sm text-gray-400">답변 작성 중…</p>}
        {message.error && <div role="alert" className="mt-2 text-sm text-red-400">
          <p>{message.error}</p>
          {onRetry && <button type="button" onClick={onRetry} className="mt-1 rounded border px-2 py-1">다시 시도</button>}
        </div>}
        {!isUser && message.sources?.length > 0 && (
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <span className="text-xs text-gray-400">참고 문서</span>
            <SourceChip source={message.sources[0]} />
          </div>
        )}
      </div>
    </div>
  );
}