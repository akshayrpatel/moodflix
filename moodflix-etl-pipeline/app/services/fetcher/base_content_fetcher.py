import logging
import requests
from abc import ABC, abstractmethod
from typing import Dict, Any, List

from app.dto.models import ContentItem, ContentItemType

logger = logging.getLogger(__name__)


class BaseContentFetcher(ABC):
	def __init__(self, api_key: str, base_url: str = "https://api.themoviedb.org/3"):
		self.tmdb_api_key = api_key
		self.base_url = base_url
		self.genres: Dict[int, str] = self._load_genres()

	@abstractmethod
	def get_type(self) -> ContentItemType:
		pass

	@abstractmethod
	def get_endpoint(self) -> str:
		"""The popular endpoint for this type (e.g., /movie/popular)."""
		pass

	@abstractmethod
	def normalize(self, data: Dict[str, Any]) -> ContentItem:
		"""Transform raw TMDB JSON into a ContentItem."""
		pass

	def fetch_page(self, page: int) -> List[Dict[str, Any]]:
		response = requests.get(
			f"{self.base_url}{self.get_endpoint()}",
			params={"api_key": self.tmdb_api_key, "page": page, "language": "en-US"},
			timeout=10
		)
		response.raise_for_status()
		return response.json().get("results", [])

	def fetch_details(self, content_id: int, path_prefix: str) -> Dict[str, Any]:
		"""Fetches detailed info + credits using append_to_response."""
		response = requests.get(
			f"{self.base_url}/{path_prefix}/{content_id}",
			params={"api_key": self.tmdb_api_key, "append_to_response": "credits", "language": "en-US"},
			timeout=10
		)
		response.raise_for_status()
		return response.json()

	def _load_genres(self) -> Dict[int, str]:
		genres = {}

		for content_type in (ContentItemType.MOVIE, ContentItemType.TV):
			response = requests.get(
				f"{self.base_url}/genre/{content_type.value}/list",
				params={"api_key": self.tmdb_api_key, "language": "en-US"},
				timeout=10,
			)
			response.raise_for_status()

			for genre in response.json().get("genres", []):
				genres[genre["id"]] = genre["name"]

		return genres
