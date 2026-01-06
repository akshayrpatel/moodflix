from enum import Enum
from typing import List, Optional, Literal
from pydantic import BaseModel
from datetime import date

class ContentItemType(str, Enum):
	MOVIE = "movie"
	TV = "tv"

class ContentItem(BaseModel):
	id: int
	title: str
	overview: Optional[str]
	content_type: ContentItemType
	release_date: Optional[date]
	release_year: Optional[int]
	language: Optional[str]
	popularity: Optional[float]
	vote_average: Optional[float]
	vote_count: Optional[int]
	genres: List[str]
	cast: List[str]
	director: Optional[str]
	poster_path: Optional[str]

	@property
	def chroma_id(self) -> str:
		return f"{self.content_type.value}_{self.id}"

	@property
	def embedding_text(self) -> str:
		return f"{self.title}. {self.overview or ''}"

class ContentPackage(BaseModel):
	content_items: List[ContentItem]
	content_type: ContentItemType
	page: int

class IngestionCheckpoint(BaseModel):
	content_type: ContentItemType
	last_page: int
	last_fetched_at: date
