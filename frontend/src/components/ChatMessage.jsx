import SourceChip from './SourceChip';

export default function ChatMessage({ message, onRetry }) {
  const isUser = message.role === 'USER';
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className="max-w-[80%] rounded-lg bg-gray-800 px-3 py-2 text-left">
        <p className="whitespace-pre-wrap">{message.content}</p>

        {/* ① 여기: 답변 작성 중 표시 */}
        {message.streaming && (
          <p className="mt-1 text-sm text-gray-400">답변 작성 중…</p>
        )}

        {/* ② 여기: 오류 문구 + 다시 시도 버튼 */}
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

        {/* ③ 기존 출처 칩 블록은 그대로 */}
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