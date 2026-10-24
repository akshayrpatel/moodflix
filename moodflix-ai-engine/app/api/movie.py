import logging
from fastapi import APIRouter, HTTPException
from app.services.service_registry import service_registry

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/internal/movies", tags=["internal"])

@router.get("/{movie_id}/embedding")
def get_movie_embedding(movie_id: str):
	"""
	Internal endpoint called by moodflix-interactions worker.
	Retrieves the pre-computed 384-dimensional embedding from ChromaDB.
	"""
	collection = service_registry.query.collection
	if not collection:
		raise HTTPException(status_code=503, detail="ChromaDB collection not initialized")

	# In MoodFlix, IDs are stored as "movie_{id}" or raw "{id}"
	candidate_ids = [f"movie_{movie_id}", movie_id]

	result = collection.get(
		ids=candidate_ids,
		include=["embeddings"]
	)

	embeddings = result.get("embeddings")
	if embeddings is None or len(embeddings) == 0:
		logger.warning(f"[Embeddings] Movie ID not found in ChromaDB: {movie_id}")
		raise HTTPException(status_code=404, detail=f"Embedding not found for movie {movie_id}")

	return {
		"movieId": movie_id,
		"embedding": embeddings[0]  # Returns the 384 float array
	}