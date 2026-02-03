from typing import List, Optional
from pydantic import BaseModel


class SearchResultItem(BaseModel):
    """
    A single semantic search hit.
    """
    content_id: int
    content_type: str

    title: str
    overview: Optional[str]
    release_year: Optional[int]
    language: Optional[str]
    poster_path: Optional[str]
    analysis: Optional[str]

class SearchResponse(BaseModel):
    """
    Response returned by /search or /recommend endpoints.
    """
    query: str
    results: List[SearchResultItem]
