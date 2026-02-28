"""
Personalized dashboard discovery service.

Generates 4 content rows from a user's taste vector using ChromaDB queries.
No LLM calls — pure vector similarity and metadata filtering.

Rows:
    1. Your Vibe Tonight   — direct taste vector similarity
    2. Hidden Gems For You  — taste vector + low popularity filter
    3. Something Different  — query by the user's least-explored mood clusters
    4. Trending Now         — high popularity + recent releases
"""

import logging
import math
from typing import List, Optional

from chromadb.api.models.Collection import Collection
from sentence_transformers import SentenceTransformer, util

from app.config.core import chroma_config
from app.dto.search_result import SearchResultItem

logger = logging.getLogger(__name__)

MOOD_LABELS = [
    "happy", "sad", "lazy", "angry", "cozy", "excited",
    "tense", "scary", "romantic", "thought-provoking", "dark", "epic",
]

ROW_SIZE = 10
FETCH_MULTIPLIER = 3  # over-fetch to have enough after dedup
HIDDEN_GEM_POPULARITY_CAP = 30
TRENDING_POPULARITY_FLOOR = 60
TRENDING_YEAR_FLOOR = 2024
BLIND_SPOT_COUNT = 3


class DiscoverService:
    """Generates personalized dashboard rows from a taste vector."""

    def __init__(self, collection: Collection) -> None:
        self.collection = collection
        self.embed_model = SentenceTransformer(
            chroma_config.embed_model_name,
        )
        self.mood_embeddings = self.embed_model.encode(
            MOOD_LABELS, convert_to_tensor=True, normalize_embeddings=True,
        )

    def discover(self, taste_vector: List[float]) -> dict:
        """Return all 4 dashboard rows for the given taste vector."""
        seen: set[int] = set()

        vibe = self._your_vibe(taste_vector, seen)
        hidden = self._hidden_gems(taste_vector, seen)
        different = self._something_different(taste_vector, seen)
        trending = self._trending_now(seen)

        return {
            "your_vibe": vibe,
            "hidden_gems": hidden,
            "something_different": different,
            "trending_now": trending,
        }

    def _your_vibe(self, taste_vector: List[float], seen: set[int]) -> List[SearchResultItem]:
        """Direct cosine similarity against the taste vector."""
        try:
            results = self.collection.query(
                query_embeddings=[taste_vector],
                n_results=ROW_SIZE * FETCH_MULTIPLIER,
            )
            return _dedup(results, seen, ROW_SIZE)
        except Exception as e:
            logger.error("[Discover] your_vibe failed: %s", e)
            return []

    def _hidden_gems(self, taste_vector: List[float], seen: set[int]) -> List[SearchResultItem]:
        """Taste vector similarity filtered to low-popularity content."""
        try:
            results = self.collection.query(
                query_embeddings=[taste_vector],
                n_results=ROW_SIZE * FETCH_MULTIPLIER,
                where={"popularity": {"$lte": HIDDEN_GEM_POPULARITY_CAP}},
            )
            items = _dedup(results, seen, ROW_SIZE)
            if len(items) < 3:
                # Not enough hidden gems — widen the popularity cap
                results = self.collection.query(
                    query_embeddings=[taste_vector],
                    n_results=ROW_SIZE * FETCH_MULTIPLIER,
                    where={"popularity": {"$lte": HIDDEN_GEM_POPULARITY_CAP * 2}},
                )
                items = _dedup(results, seen, ROW_SIZE)
            return items
        except Exception as e:
            logger.error("[Discover] hidden_gems failed: %s", e)
            return []

    def _something_different(self, taste_vector: List[float], seen: set[int]) -> List[SearchResultItem]:
        """Find the user's blind-spot moods and query content from those moods."""
        try:
            import torch
            taste_tensor = torch.tensor(taste_vector).unsqueeze(0)

            similarities = util.cos_sim(taste_tensor, self.mood_embeddings)[0]
            scores = similarities.cpu().flatten().tolist()

            # Find the least-similar moods (blind spots)
            mood_scores = sorted(
                zip(MOOD_LABELS, scores), key=lambda x: x[1],
            )
            blind_spots = [mood for mood, _ in mood_scores[:BLIND_SPOT_COUNT]]

            logger.info("[Discover] Blind-spot moods: %s", blind_spots)

            query_text = f"movies that feel {', '.join(blind_spots)}"
            results = self.collection.query(
                query_texts=[query_text],
                n_results=ROW_SIZE * FETCH_MULTIPLIER,
            )
            return _dedup(results, seen, ROW_SIZE)
        except Exception as e:
            logger.error("[Discover] something_different failed: %s", e)
            return []

    def _trending_now(self, seen: set[int]) -> List[SearchResultItem]:
        """High popularity + recent releases. Not personalized."""
        try:
            results = self.collection.query(
                query_texts=["popular acclaimed movies and shows"],
                n_results=ROW_SIZE * FETCH_MULTIPLIER,
                where={
                    "$and": [
                        {"popularity": {"$gte": TRENDING_POPULARITY_FLOOR}},
                        {"release_year": {"$gte": TRENDING_YEAR_FLOOR}},
                    ]
                },
            )
            items = _dedup(results, seen, ROW_SIZE)
            if len(items) < 3:
                results = self.collection.query(
                    query_texts=["popular acclaimed movies and shows"],
                    n_results=ROW_SIZE * FETCH_MULTIPLIER,
                    where={"popularity": {"$gte": TRENDING_POPULARITY_FLOOR}},
                )
                items = _dedup(results, seen, ROW_SIZE)
            return items
        except Exception as e:
            logger.error("[Discover] trending_now failed: %s", e)
            return []


def _dedup(results, seen: set[int], limit: int) -> List[SearchResultItem]:
    """Map ChromaDB results to SearchResultItem list, skipping already-seen content."""
    if not results or not results.get("ids") or not results["ids"][0]:
        return []

    items: List[SearchResultItem] = []
    metadatas = results.get("metadatas", [[]])[0]
    documents = results.get("documents", [[]])[0]
    for meta, doc in zip(metadatas, documents):
        content_id = int(meta.get("tmdb_id", 0))
        if content_id in seen:
            continue
        seen.add(content_id)
        items.append(
            SearchResultItem(
                content_id=content_id,
                content_type=meta.get("content_type", "movie"),
                title=meta.get("title", "Unknown"),
                overview=doc or "",
                release_year=meta.get("release_year"),
                language=meta.get("language"),
                poster_path=meta.get("poster_path"),
                analysis=meta.get("analysis"),
            )
        )
        if len(items) >= limit:
            break
    return items
