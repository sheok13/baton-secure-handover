export default function SourceChip({ source }) {
  return (
    <span className="inline-flex min-h-7 items-center rounded-md border border-field px-2.5 text-[13px] text-[#3A4458]">
      {source.title}
      {source.page != null && ` · ${source.page}쪽`}
    </span>
  );
}
