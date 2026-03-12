import { ContentItem } from "@/types";

/**
 * Communicates with the MoodFlix API via Next.js Rewrites.
 * Since we are using a proxy in next.config, we use a relative path
 * to avoid CORS issues and keep internal architecture hidden.
 */
export const searchMovies = async (query: string): Promise<ContentItem[]> => {
  if (!query || query.trim().length < 3) return [];

  try {
    const response = await fetch("/api/search", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ query }),
    });

    if (!response.ok) {
      throw new Error(`MoodFlix API error: ${response.status}`);
    }

    const data = await response.json();

    return (data.results || []).map((item: ContentItem) => ({
      content_id: item.content_id,
      content_type: item.content_type,
      title: item.title,
      overview: item.overview,
      release_year: item.release_year,
      poster_path: item.poster_path || null,
      analysis: item.analysis || "",
    }));
  } catch (error) {
    console.error("Search service failure:", error);
    return [];
  }
};
