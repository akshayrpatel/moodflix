from datetime import date
from typing import Dict, Any

from app.dto.models import ContentItem, ContentItemType
from app.services.fetcher.base_content_fetcher import BaseContentFetcher


class MovieFetcher(BaseContentFetcher):
	def get_type(self) -> ContentItemType:
		return ContentItemType.MOVIE

	def get_endpoint(self) -> str:
		return "/movie/popular"

	def normalize(self, data: Dict[str, Any]) -> ContentItem:
		movie_credits = data.get("credits", {})
		cast = [m["name"] for m in movie_credits.get("cast", [])[:5]]
		crew = movie_credits.get("crew", [])
		director = next((m["name"] for m in crew if m["job"] == "Director"), "Unknown")
		rel_date = data.get("release_date")

		return ContentItem(
			id=data["id"],
			title=data["title"],
			overview=data.get("overview"),
			content_type=ContentItemType.MOVIE,
			release_date=date.fromisoformat(rel_date) if rel_date else None,
			release_year=int(rel_date[:4]) if rel_date else None,
			language=data.get("original_language", "en"),
			popularity=data.get("popularity", 0.0),
			vote_average=data.get("vote_average", 0.0),
			vote_count=data.get("vote_count", 0),
			genres=[g["name"] for g in data.get("genres", [])],
			cast=cast,
			director=director,
			poster_path=data.get("poster_path"),
		)
