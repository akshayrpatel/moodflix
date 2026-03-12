import { MOOD_CONFIG } from "@/data/moods";
import { ContentItem } from "@/types";

const CACHE_KEY = "mood_flix_cache";

export const moodService = {
  // Helper to get entire cache from LocalStorage
  getStoredCache(): Record<string, ContentItem[]> {
    if (typeof window === "undefined") return {};
    const saved = localStorage.getItem(CACHE_KEY);
    return saved ? JSON.parse(saved) : {};
  },

  // Helper to save a single mood to LocalStorage
  setStoredCache(value: string, data: ContentItem[]) {
    const cache = this.getStoredCache();
    cache[value] = data;
    localStorage.setItem(CACHE_KEY, JSON.stringify(cache));
  },

  async warmupCache(): Promise<void> {
    const allPrompts = MOOD_CONFIG.map((m) => m.prompt);

    try {
      const response = await fetch("/api/search/batch", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ queries: allPrompts }),
      });

      if (!response.ok) throw new Error("Batch fetch failed");

      const data = await response.json();
      // data.results is List[List[SearchResultItem]]

      MOOD_CONFIG.forEach((mood, index) => {
        const movieResults = data.results[index];
        if (movieResults && movieResults.length > 0) {
          this.setStoredCache(mood.value, movieResults);
        }
      });
    } catch (error) {
      console.error("Bacth service failed :", error);
    }
  },
};
