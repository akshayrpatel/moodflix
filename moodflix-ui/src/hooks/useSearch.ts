import { useState } from "react";
import { ContentItem, SearchStatus } from "@/types";
import { searchMovies } from "@/services/search";

export const useSearch = () => {
  const [query, setQuery] = useState("");
  const [searchResults, setSearchResults] = useState<ContentItem[]>([]);
  const [searchStatus, setSearchStatus] = useState<SearchStatus>("idle");

  const syncQuery = (val: string) => {
    setQuery(val);
    if (!val.trim()) {
      setSearchResults([]);
      setSearchStatus("idle");
    }
  };

  const triggerSearch = async (val?: string) => {
    const target = val ?? query;
    if (!target.trim()) return;

    setSearchStatus("searching");
    try {
      const data = await searchMovies(target);
      setSearchResults(data);
      setSearchStatus("completed");
    } catch (err) {
      console.error("Search Error:", err);
      setSearchStatus("idle");
    }
  };

  return {
    query,
    syncQuery,
    triggerSearch,
    searchStatus,
    searchResults,
  };
};
