import { FC } from "react";

const ContentCardSkeleton: FC = () => {
  return (
    <div className="flex flex-col w-full gap-3">
      <div className="relative aspect-2/3 w-full overflow-hidden rounded-sm bg-zinc-800 animate-pulse">
        <div className="absolute inset-0 -translate-x-full animate-[shimmer_2s_infinite] bg-linear-to-r from-transparent via-white/5 to-transparent" />
      </div>
      <div className="h-4 w-3/4 rounded-sm bg-zinc-800 animate-pulse" />
    </div>
  );
};

export default ContentCardSkeleton;
