from datetime import date
from typing import Dict, Any

from app.dto.models import ContentItem, ContentItemType
from app.services.fetcher.base_content_fetcher import BaseContentFetcher


class TVFetcher(BaseContentFetcher):
	def get_type(self) -> ContentItemType:
		return ContentItemType.TV

	def get_endpoint(self) -> str:
		return "/tv/popular"

	def normalize(self, data: Dict[str, Any]) -> ContentItem:
		tv_credits = data.get("credits", {})
		cast = [m["name"] for m in tv_credits.get("cast", [])[:5]]
		# TV shows usually have 'Created By' instead of a single 'Director'
		creator = data.get("created_by", [{}])[0].get("name", "Unknown") if data.get("created_by") else "Unknown"
		first_air = data.get("first_air_date")

		return ContentItem(
			id=data["id"],
			title=data["name"],  # TV uses 'name' instead of 'title'
			overview=data.get("overview"),
			content_type=ContentItemType.TV,
			release_date=date.fromisoformat(first_air) if first_air else None,
			release_year=int(first_air[:4]) if first_air else None,
			language=data.get("original_language", "en"),
			popularity=data.get("popularity", 0.0),
			vote_average=data.get("vote_average", 0.0),
			vote_count=data.get("vote_count", 0),
			genres=[g["name"] for g in data.get("genres", [])],
			cast=cast,
			director=creator,
			poster_path=data.get("poster_path"),
		)