from fastapi import APIRouter
from typing import List

from pydantic import BaseModel

from app.dto.search_result import SearchResultItem
from app.services.service_registry import service_registry

router = APIRouter()

class SearchRequest(BaseModel):
	query: str

class BatchRequest(BaseModel):
	queries: List[str]

@router.post("/search")
def search(request: SearchRequest):
	results: List[SearchResultItem] = service_registry.query.search(request.query)
	return {
		"query": request.query,
		"results": results
	}

@router.post("/search/batch")
async def search_batch(request: BatchRequest):
	results: List[List[SearchResultItem]] = service_registry.query.search_batch(request.queries)
	return {
		"queries": request.queries,
		"results": results
	}
