// Create a new file: @/utils/images.ts or just add to your types
export const TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/";

export const getPosterUrl = (
  path: string | null,
  size: "w500" | "original" = "w500",
) => {
  if (!path) return "/placeholder-poster.png";
  return `${TMDB_IMAGE_BASE}${size}${path}`;
};
