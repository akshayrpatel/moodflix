import { ContentItem } from "@/types";
import Image from "next/image";
import { getPosterUrl } from "../utils/images";
import { FC } from "react";

interface ContentCardProps {
  content_item: ContentItem;
}

const ContentCard: FC<ContentCardProps> = ({ content_item }) => {
  const posterUrl = getPosterUrl(content_item.poster_path, "w500");

  return (
    <div
      className="group relative aspect-2/3 rounded-xl overflow-hidden bg-zinc-900 cursor-pointer 
                    transition-all duration-300 ease-out 
                    hover:scale-105 hover:z-50 
                    shadow-md hover:shadow-2xl hover:shadow-black"
    >
      <Image
        src={posterUrl}
        alt={content_item.title}
        fill
        /* Image zooms slightly more (110) than the container (105) for depth */
        className="object-cover transition-all duration-500 group-hover:scale-110 group-hover:opacity-40"
        sizes="(max-width: 768px) 160px, 240px"
      />

      {/* Text Overlay - Year, Title, Overview */}
      <div
        className="absolute inset-0 bg-linear-to-t from-black via-black/60 to-transparent 
                      opacity-0 group-hover:opacity-100 transition-opacity duration-300 
                      flex flex-col justify-end p-4"
      >
        <span className="text-[10px] text-zinc-400 font-semibold mb-1 uppercase tracking-wider">
          {content_item.release_year || "2025"}
        </span>

        <h3 className="text-white text-sm font-bold leading-tight mb-1">
          {content_item.title}
        </h3>

        <p className="text-[10px] text-zinc-300 line-clamp-2 leading-relaxed">
          {content_item.overview}
        </p>
      </div>

      {/* Static shadow when not hovered */}
      <div
        className="absolute inset-x-0 bottom-0 h-1/4 bg-linear-to-t from-black/80 to-transparent 
                      group-hover:opacity-0 transition-opacity duration-300 pointer-events-none"
      />
    </div>
  );
};

export default ContentCard;
