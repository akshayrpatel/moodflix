export type SearchStatus = "idle" | "searching" | "completed";

export interface ContentItem {
  content_id: number;
  content_type: string;
  title: string;
  release_year: string;
  poster_path: string;
  overview?: string;
  analysis?: string;
}

export interface Mood {
  id: string;
  value: string;
  label: string;
  prompt: string;
}
