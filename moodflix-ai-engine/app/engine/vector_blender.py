"""
Vector blending utility for personalized search.

Combines a query embedding with a user's taste vector to produce a
personalized search vector. This shifts ChromaDB results toward content
that matches both what the user asked for AND what they historically enjoy.

How it works
------------
Given two L2-normalized 384-dimensional vectors:

    query_vector  — the embedded representation of the user's search text
    user_vector   — the user's taste profile (stored in PostgreSQL, injected
                    by the gateway via the X-User-Vector header)

The blended vector is computed as a weighted linear interpolation:

    blended = alpha * query_vector + (1 - alpha) * user_vector

Where alpha (default 0.7) controls the balance:

    alpha = 1.0  →  pure query, no personalization
    alpha = 0.7  →  query-dominant with a gentle taste bias  (default)
    alpha = 0.5  →  equal weight to query and taste
    alpha = 0.0  →  pure taste, ignores the query entirely

After blending, the result is L2-normalized back to unit length. This is
required because ChromaDB's SentenceTransformerEmbeddingFunction produces
normalized embeddings, and cosine similarity on unit vectors reduces to a
dot product — mixing unnormalized vectors would distort distance rankings.

Why linear interpolation?
    Simpler alternatives (e.g., concatenation, re-ranking) either change the
    vector dimensionality or require two separate ChromaDB calls. Weighted
    interpolation keeps the same 384-dim space, works in a single query, and
    the alpha knob gives direct control over personalization strength.
"""

import logging
import math

logger = logging.getLogger(__name__)

DEFAULT_BLEND_ALPHA = 0.7


class VectorBlender:
    """Blends query and user taste vectors for personalized search.

    Stateless utility — alpha is set at construction time and reused
    across all calls. Instantiate once in QueryService and call blend()
    per search request.

    Args:
        alpha: Blending weight in [0, 1]. Higher values favor the query
               vector; lower values favor the user's taste vector.
               Defaults to DEFAULT_BLEND_ALPHA (0.7).
    """

    def __init__(self, alpha: float = DEFAULT_BLEND_ALPHA) -> None:
        if not 0.0 <= alpha <= 1.0:
            raise ValueError(f"alpha must be in [0, 1], got {alpha}")
        self.alpha = alpha

    def blend(
        self,
        query_vector: list[float],
        user_vector: list[float],
    ) -> list[float]:
        """Blend a query vector with a user taste vector.

        Computes:  normalize(alpha * query + (1 - alpha) * user)

        Args:
            query_vector: Embedded search query (384-dim, L2-normalized).
            user_vector:  User taste profile  (384-dim, L2-normalized).

        Returns:
            A new L2-normalized 384-dim vector biased toward the user's taste.

        Raises:
            ValueError: If the vectors have different dimensions.
        """
        if len(query_vector) != len(user_vector):
            raise ValueError(
                f"Dimension mismatch: query has {len(query_vector)}, "
                f"user has {len(user_vector)}"
            )

        a = self.alpha
        b = 1.0 - a

        blended = [a * q + b * u for q, u in zip(query_vector, user_vector)]

        norm = math.sqrt(sum(x * x for x in blended))
        if norm == 0.0:
            logger.warning("[VectorBlender] Zero-norm after blending — returning zero vector")
            return blended

        return [x / norm for x in blended]
