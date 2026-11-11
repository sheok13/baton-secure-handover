import { groupSessionsByDate } from '../utils/groupByDate';
export default function ChatSidebar({ sessions, selectedId, onSelect, onNew }) {
  return (
    <aside className="w-64 shrink-0 border-r border-gray-700 p-3">
      <h2 className="mb-2 text-sm px-2 font-bold text-gray-300 text-left">대화 목록</h2>
      <button
        type="button"
        onClick={onNew}
        className="mb-2 w-full rounded bg-blue-600 px-2 py-1 text-sm text-white hover:bg-blue-700"
      >
        새 대화
      </button>
      {sessions.length === 0 && (
        <p className="text-sm text-gray-500">대화가 없습니다.</p>
      )}
         {groupSessionsByDate(sessions).map((g) => (
     <section key={g.label} className="mb-3">
       <h3 className="mb-1 text-xs px-2 text-gray-500 text-left">{g.label}</h3>
       <ul className="space-y-1">
         {g.items.map((s) => (
           <li key={s.id}>
             <button
               type="button"
               onClick={() => onSelect(s.id)}
               className={`w-full rounded px-2 py-1 text-left text-sm ${
                 s.id === selectedId ? 'bg-gray-700 text-white' : 'text-gray-300 hover:bg-gray-800'
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