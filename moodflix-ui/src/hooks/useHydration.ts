import { useState, useEffect } from "react";
import { ContentItem } from "@/types";
import { MOOD_CONFIG } from "@/data/moods";
import { moodService } from "@/services/moodService";

export type HydrationStatus = "warming" | "ready" | "error";

export const useHydration = () => {
  const [featuredSections, setFeaturedSections] = useState<
    { title: string; movies: ContentItem[] }[]
  >([]);
  const [hydrationStatus, setHydrationStatus] =
    useState<HydrationStatus>("warming");

  useEffect(() => {
    const initialize = async () => {
      try {
        const shuffled = [...MOOD_CONFIG]
          .sort(() => Math.random() - 0.5)
          .slice(0, 4);

        // Cache
        const existingCache = moodService.getStoredCache();
        if (Object.keys(existingCache).length === 0) {
          await moodService.warmupCache();
        } else {
          moodService.warmupCache();
        }

        // Data Mapping
        const finalCache = moodService.getStoredCache();
        const rows = shuffled.map((mood) => ({
          title: mood.label,
          movies: finalCache[mood.value] || [],
        }));

        setFeaturedSections(rows);
        setHydrationStatus("ready");
      } catch (err) {
        console.error("Hydration Error:", err);
        setHydrationStatus("error");
      }
    };

    initialize();
  }, []);

  return { featuredSections, hydrationStatus };
};
