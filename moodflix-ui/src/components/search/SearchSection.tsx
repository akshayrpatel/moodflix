"use client";

import { FC } from "react";
import SearchTags from "./SearchTags";
import SearchBar from "./SearchBar";

interface SearchSectionProps {
  currentQuery: string;
  onQueryChange: (val: string) => void;
  onTriggerSearch: (val: string) => void;
  isSearching: boolean;
}

const SearchSection: FC<SearchSectionProps> = ({
  currentQuery,
  onQueryChange,
  onTriggerSearch,
  isSearching,
}) => {
  return (
    <section className="w-full max-w-3xl mx-auto px-4 z-10">
      <SearchBar
        onQueryChange={onQueryChange}
        onTriggerSearch={onTriggerSearch}
        initialValue={currentQuery}
      />
      <SearchTags onTagClick={onQueryChange} activeQuery={currentQuery} />
    </section>
  );
};

export default SearchSection;
