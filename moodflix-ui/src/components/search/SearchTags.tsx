import { FC } from "react";

interface MoodTag {
  label: string;
  searchQuery: string;
}

const TAGS: MoodTag[] = [
  {
    label: "Existential Crisis",
    searchQuery: "movies about the meaning of life and cosmic insignificance",
  },
  {
    label: "Pure Brain Rot",
    searchQuery: "absurd comedy and colorful non-stop action",
  },
  {
    label: "Visually Stunning",
    searchQuery: "cinematography masterpieces with beautiful color palettes",
  },
  {
    label: "Emotional Damage",
    searchQuery: "soul-crushing dramas that will make me cry",
  },
  {
    label: "Hidden Gems",
    searchQuery: "underrated indie movies with high ratings",
  },
];

interface SearchTagsProps {
  onTagClick: (tag: string) => void;
  activeQuery?: string;
}

const SearchTags: FC<SearchTagsProps> = ({ onTagClick, activeQuery }) => {
  return (
    <div className="flex flex-wrap w-full mt-6 gap-2 justify-center animate__animated animate__fadeInUp animate__delay-1s">
      {TAGS.map((tag) => {
        const isActive = activeQuery === tag.searchQuery;

        return (
          <button
            key={tag.label}
            onClick={() => onTagClick(tag.searchQuery)}
            className={`flex h-8 md:h-10 px-2 md:px-4 py-1.5 cursor-pointer rounded-full items-center justify-center border text-xs md:text-sm font-medium transition-all active:scale-95
              ${
                isActive
                  ? "border-brand-red bg-white/10 backdrop-blur-sm text-brand-red"
                  : "border-white/10 bg-white/5 backdrop-blur-sm text-zinc-400 hover:border-brand-red/50 hover:text-brand-red"
              }`}
          >
            <span className="text-sm font-medium whitespace-nowrap">
              {tag.label}
            </span>
          </button>
        );
      })}
    </div>
  );
};

export default SearchTags;
