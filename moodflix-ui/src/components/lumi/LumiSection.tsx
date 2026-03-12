import React, { FC } from "react";
import ContentCard from "../content/ContentCard";
import { ContentItem, SearchStatus } from "@/types";

interface LumiSectionProps {
  recommendedContentItem: ContentItem;
  searchStatus: SearchStatus;
}

const LumiSection: FC<LumiSectionProps> = ({
  recommendedContentItem,
  searchStatus,
}) => {
  if (recommendedContentItem?.analysis === "") return null;
  if (!recommendedContentItem && searchStatus !== "searching") return null;

  return (
    <div className="w-full">
      <div className="relative overflow-hidden rounded-2xl  border border-zinc-900/60  bg-white/5 backdrop-blur-md">
        <div className="absolute -left-10 inset-y-0 w-40 bg-red-600/10 blur-3xl pointer-events-none" />

        <div className="flex flex-row items-stretch min-h-48">
          {/* COLUMN 1: Character */}
          <div className="w-24 md:w-32 shrink-0 flex flex-col items-center justify-center">
            <div className="relative">
              <span className="inline-block text-4xl md:text-5xl drop-shadow-md transition-transform duration-300 ease-out hover:scale-125 cursor-pointer">
                {"🦑"}
              </span>
              <div className="absolute -bottom-1 -right-1 w-2.5 h-2.5 bg-red-600 rounded-full border-2 border-zinc-900 animate-pulse" />
            </div>
          </div>

          {/* COLUMN 2: Analysis & Title */}
          <div className="flex-1 p-6 md:p-10 flex flex-col justify-center min-w-0 text-left">
            <div className="flex items-center gap-2 mb-4">
              <div className="h-0.5 w-3 bg-red-600" />
              <span className="text-[10px] font-bold uppercase tracking-[0.3em] text-red-500">
                {"Lumi's Pick"}
              </span>
            </div>

            {searchStatus === "searching" ? (
              <div className="space-y-3 animate-pulse">
                <div className="h-4 bg-white/10 rounded w-full" />
                <div className="h-4 bg-white/10 rounded w-2/3" />
              </div>
            ) : (
              <div className="space-y-6">
                <p className="text-sm md:text-lg text-zinc-100 font-light italic leading-relaxed">
                  {recommendedContentItem?.analysis}
                </p>

                {/* Movie Title Tag */}
                {/* <div className="inline-flex items-center gap-3 py-2 rounded-lg shadow-xl">
									<span className="text-xs font-bold text-white uppercase tracking-wider">
										{recommendedContentItem?.title}
									</span>
									<span className="text-[10px] text-zinc-500 font-mono">
										{recommendedContentItem?.release_year}
									</span>
								</div> */}
                {/* <div className="flex flex-wrap gap-2">
									{recommendedContentItem?.moods
										?.split(",")
										.map((mood: string) => (
											<span
												key={mood}
												className="px-3 py-0.5 rounded-full bg-zinc-800 border border-zinc-700 text-md font-medium text-zinc-200 capitalize tracking-wide"
											>
												{mood.trim()}
											</span>
										))}
								</div> */}
              </div>
            )}
          </div>

          {/* COLUMN 3: Content Card */}

          <div className="w-48 md:w-56 shrink-0 p-4 flex flex-col items-center justify-center">
            <div className="w-full relative">
              <ContentCard content_item={recommendedContentItem} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LumiSection;
