export default function SkeletonCard() {
  return (
    <div className="card overflow-hidden flex flex-col animate-pulse">
      <div className="w-full aspect-[2/3] bg-gray-200 dark:bg-gray-700 rounded-xl mb-3" />
      <div className="px-1 pb-1 space-y-2">
        <div className="h-3.5 bg-gray-200 dark:bg-gray-700 rounded-full w-4/5" />
        <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full w-3/5" />
      </div>
    </div>
  );
}
