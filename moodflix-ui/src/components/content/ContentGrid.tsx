import { FC } from "react";
import { ContentItem } from "@/types";
import ContentCard from "./ContentCard";
import ContentCardSkeleton from "./ContentCardSkeleton";

interface ContentGridProps {
  isLoading?: boolean;
  title: string;
  movies: ContentItem[];
}

const ContentGrid: FC<ContentGridProps> = ({ isLoading, title, movies }) => {
  return (
    <section className="flex flex-col w-full max-w-400 mx-auto px-8 lg:px-16 pb-20">
      <div className="flex mb-8 items-center gap-4">
        <h2 className="text-2xl font-bold tracking-tight text-zinc-200">
          {title}
        </h2>
        <div className="flex-1 h-px bg-zinc-800" />
        <div className="w-12 h-0.5 bg-brand-red" />
      </div>

      <div className="grid w-full grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
        {isLoading ? (
          Array.from({ length: 6 }).map((_, i) => (
            <ContentCardSkeleton key={i} />
          ))
        ) : movies.length > 0 ? (
          movies.map((movie) => (
            <ContentCard key={movie.content_id} content_item={movie} />
          ))
        ) : (
          <div className="col-span-full py-20 items-center text-center">
            <p className="text-xl font-medium text-zinc-600 italic">
              No matches found.
            </p>
          </div>
        )}
      </div>
    </section>
  );
};

export default ContentGrid;
