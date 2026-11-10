export default function SourceChip({ source }) {
  return (
    <span className="rounded-full border border-gray-600 px-2 py-0.5 text-xs text-gray-300">
      {source.title}
      {source.page != null && ` · ${source.page}쪽`}
    </span>
  );
}