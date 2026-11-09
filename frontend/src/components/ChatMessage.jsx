import SourceChip from './SourceChip';

export default function ChatMessage({ message }) {
  const isUser = message.role === 'USER';
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className="max-w-[80%] rounded-lg bg-gray-800 px-3 py-2 text-left">
        <p className="whitespace-pre-wrap">{message.content}</p>
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