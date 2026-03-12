import { ContentItem } from "@/types";
import { FC } from "react";
import ContentRow from "./ContentRow";

interface ContentGridSectionProps {
  query: string;
  searchResults: ContentItem[];
  sections: { title: string; movies: ContentItem[] }[];
  isSearching: boolean;
  hydrationStatus: "warming" | "ready" | "error";
}

const ContentGridSection: FC<ContentGridSectionProps> = ({
  query,
  searchResults,
  sections,
  isSearching,
  hydrationStatus,
}) => {
  const hasInput = query.trim().length > 0;

  // 1. Search Mode
  if (hasInput) {
    return (
      <div className="flex flex-col w-full gap-8 mt-10 pb-20 animate-in fade-in duration-500">
        <ContentRow
          title={isSearching ? `Searching...` : `Results for "${query}"`}
          content_items={searchResults}
          isLoading={isSearching}
        />
      </div>
    );
  }

  // 2. Initial Loading Mode (Warmup)
  const isActuallyReady =
    hydrationStatus === "ready" && sections.some((s) => s.movies.length > 0);
  if (hydrationStatus === "warming" || !isActuallyReady) {
    return (
      <div className="flex flex-col w-full gap-12 mt-10 pb-20">
        <div className="h-64 w-full bg-white/5 animate-pulse rounded-2xl" />
        <div className="h-64 w-full bg-white/5 animate-pulse rounded-2xl" />
      </div>
    );
  }

  // 3. Ready Mode (Mood Rows)
  return (
    <div className="flex flex-col w-full gap-16 mt-10 pb-20 animate-in fade-in slide-in-from-bottom-6 duration-1000 ease-out">
      {sections.map((section, idx) => (
        <ContentRow
          key={idx}
          title={section.title}
          content_items={section.movies}
        />
      ))}
    </div>
  );
};
export default ContentGridSection;
