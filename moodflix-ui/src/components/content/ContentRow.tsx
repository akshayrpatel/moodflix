"use client";

import { FC, useRef } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { ContentItem } from "@/types";
import ContentCard from "./ContentCard";
import ContentCardSkeleton from "./ContentCardSkeleton";

interface ContentRowProps {
  isLoading?: boolean;
  title: string;
  content_items: ContentItem[];
}

const ContentRow: FC<ContentRowProps> = ({
  isLoading,
  title,
  content_items,
}) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  const scroll = (direction: "left" | "right") => {
    if (scrollRef.current) {
      const { scrollLeft, clientWidth } = scrollRef.current;
      const scrollTo =
        direction === "left"
          ? scrollLeft - clientWidth
          : scrollLeft + clientWidth;
      scrollRef.current.scrollTo({ left: scrollTo, behavior: "smooth" });
    }
  };

  return (
    <section className="flex flex-col w-full mb-12 group/row overflow-visible animate__animated animate__fadeIn">
      <div className="flex mb-6 items-center justify-between">
        <div className="flex items-center gap-4 flex-1">
          <h2 className="text-xl md:text-2xl font-bold tracking-tight text-zinc-200 whitespace-nowrap">
            {title}
          </h2>
          <div className="flex-1 h-px bg-zinc-800/50" />
          <div className="w-12 h-1 bg-red-600 rounded-full shadow-[0_0_10px_rgba(255,0,0,0.5)]" />
        </div>

        {/* Navigation - now listens to group-hover/row */}
        <div className="hidden md:flex gap-2 ml-6 opacity-0 group-hover/row:opacity-100 transition-opacity">
          <button
            onClick={() => scroll("left")}
            className="p-2 rounded-full bg-white/5 border border-white/10 hover:bg-white/10 transition-colors"
          >
            <ChevronLeft size={20} className="text-white" />
          </button>
          <button
            onClick={() => scroll("right")}
            className="p-2 rounded-full bg-white/5 border border-white/10 hover:bg-white/10 transition-colors"
          >
            <ChevronRight size={20} className="text-white" />
          </button>
        </div>
      </div>

      <div
        ref={scrollRef}
        className="flex w-full 
        px-8 lg:px-16 py-10 -my-10 gap-6
        overflow-x-auto overflow-y-visible   
        scrollbar-hide snap-x snap-mandatory scroll-smooth"
      >
        {isLoading ? (
          Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="min-w-40 md:min-w-60">
              <ContentCardSkeleton />
            </div>
          ))
        ) : content_items.length > 0 ? (
          content_items.map((content_item, index) => (
            <div
              key={content_item.content_id}
              className="min-w-40 md:min-w-60 snap-start animate__animated animate__fadeInUp"
              style={{
                animationDelay: `${index * 0.1}s`,
                animationFillMode: "both",
              }}
            >
              <ContentCard content_item={content_item} />
            </div>
          ))
        ) : (
          <div className="w-full py-10 text-center text-zinc-600 italic">
            No matches for this mood.
          </div>
        )}
      </div>
    </section>
  );
};

export default ContentRow;
