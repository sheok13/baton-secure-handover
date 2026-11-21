import { groupSessionsByDate } from '../utils/groupByDate';

export default function ChatSidebar({ sessions, selectedId, onSelect, onNew }) {
  return (
    <aside className="flex w-60 shrink-0 flex-col gap-2 overflow-y-auto rounded-xl border border-line bg-white p-4">
      <span className="text-sm font-bold">대화 기록</span>
      <button
        type="button"
        onClick={onNew}
        className="min-h-11 rounded-lg border border-brand bg-white px-3 text-sm font-medium text-brand hover:bg-brand-soft"
      >
        + 새 대화
      </button>
      {sessions.length === 0 && <p className="text-sm text-sub">대화가 없습니다.</p>}
      {groupSessionsByDate(sessions).map((g) => (
        <section key={g.label} className="mt-1 flex flex-col gap-1">
          <h3 className="px-3 text-xs text-sub">{g.label}</h3>
          <ul className="flex flex-col gap-1">
            {g.items.map((s) => (
              <li key={s.id}>
                <button
                  type="button"
                  onClick={() => onSelect(s.id)}
                  className={`min-h-11 w-full truncate rounded-lg px-3 py-2 text-left text-sm ${
                    s.id === selectedId
                      ? 'bg-brand-soft font-medium text-brand-dark'
                      : 'text-ink hover:bg-bg'
                  }`}
                >
                  {s.title}
                </button>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </aside>
  );
}
