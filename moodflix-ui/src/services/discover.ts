import { ContentItem } from "@/types";

export interface DiscoverRow {
  key: string;
  title: string;
  items: ContentItem[];
}

export interface DiscoverResponse {
  rows: DiscoverRow[];
}

export const fetchDiscover = async (): Promise<DiscoverRow[]> => {
  try {
    const res = await fetch("/api/discover", {
      method: "GET",
      headers: { "Content-Type": "application/json" },
    });
    if (!res.ok) return [];
    const data: DiscoverResponse = await res.json();
    return data.rows || [];
  } catch (err) {
    console.error("Discover service failure:", err);
    return [];
  }
};
