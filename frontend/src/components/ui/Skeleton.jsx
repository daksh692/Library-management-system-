/** Grey pulsing block. Compose these into shapes rather than styling each one inline. */
export const Skeleton = ({ className = '' }) => (
  <div className={`animate-pulse bg-slate-200 rounded ${className}`} aria-hidden="true" />
);

/** Placeholder rows for a table body. */
export const TableSkeleton = ({ rows = 5, cols = 4 }) => (
  <>
    {Array.from({ length: rows }).map((_, r) => (
      <tr key={r}>
        {Array.from({ length: cols }).map((__, c) => (
          <td key={c} className="px-6 py-4">
            <Skeleton className={`h-4 ${c === 0 ? 'w-24' : c === 1 ? 'w-48' : 'w-16'}`} />
          </td>
        ))}
      </tr>
    ))}
  </>
);

/** Placeholder grid for the patron book cards. */
export const BookCardSkeleton = () => (
  <div className="flex bg-white border border-slate-100 rounded-lg overflow-hidden h-48">
    <Skeleton className="w-1/3 h-full rounded-none" />
    <div className="w-2/3 p-4 space-y-3">
      <Skeleton className="h-3 w-16" />
      <Skeleton className="h-5 w-3/4" />
      <Skeleton className="h-3 w-1/2" />
      <Skeleton className="h-3 w-full" />
      <Skeleton className="h-3 w-5/6" />
    </div>
  </div>
);
