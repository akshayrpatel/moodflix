import logging
from typing import List
from chromadb.utils import embedding_functions

from app.config.core import chroma_config

logger = logging.getLogger(__name__)

# Maps each mood combination to a descriptive text
# that captures the cinematic feeling in embedding space
MOOD_DESCRIPTIONS = {
	"NEED_TO_FEEL": "emotionally devastating deeply moving tearjerker profound human drama",
	"EDGE_OF_SEAT": "suspenseful thriller intense action high stakes adrenaline",
	"JUST_LAUGH": "comedy funny lighthearted hilarious entertaining fun",
	"MIND_BLOWN": "mind bending cerebral complex narrative twist psychological",
	"QUIET_BEAUTIFUL": "slow burn arthouse contemplative visually stunning atmospheric",
	"PURE_CHAOS": "explosive action chaos energy wild unpredictable frenetic",
	"LEARN_SOMETHING": "documentary educational biographical historical true story",
}

COMPANY_DESCRIPTIONS = {
	"SOLO": "introspective personal intimate",
	"DATE_NIGHT": "romantic engaging crowd pleasing",
	"FAMILY": "family friendly wholesome all ages",
	"FRIENDS": "fun social entertaining group",
}

COMMITMENT_DESCRIPTIONS = {
	"CASUAL": "easy watching light entertaining",
	"FOCUSED": "immersive engaging requires attention",
	"COMFORT_REWATCH": "familiar classic beloved rewatchable",
	"EPIC": "grand epic long ambitious masterpiece",
}

# Weights for each category — mood matters most
MOOD_WEIGHT = 0.6
COMPANY_WEIGHT = 0.2
COMMITMENT_WEIGHT = 0.2


class OnboardingService:
	"""
	Computes a seed taste vector from a user's mood selections.

	Uses the same SentenceTransformer embedding model as QueryService
	to ensure vectors live in the same embedding space as movie vectors.
	Mood, company, and commitment selections are mapped to descriptive
	text, embedded, and combined using weighted averaging.
	"""

	def __init__(self) -> None:
		logger.info("[OnboardingService] Initializing embedding function")
		self.embedding_function = (
			embedding_functions.SentenceTransformerEmbeddingFunction(
				model_name=chroma_config.embed_model_name,
				normalize_embeddings=True,
			)
		)
		logger.info("[OnboardingService] Ready")

	def compute_seed_vector(
					self,
					mood: str,
					company: str,
					commitment: str,
					tired_of: List[str],
	) -> tuple[List[float], float]:
		"""
		Computes a seed taste vector from mood selections.

		Maps each selection to descriptive text, embeds each text
		separately, then combines using weighted averaging. Returns
		both the vector and a confidence score.

		Confidence is higher when selections map to a tight,
		specific region of taste space.

		Args:
				mood:       the user's mood tonight
				company:    who they are watching with
				commitment: how deep they want to go
				tired_of:   what they have had enough of

		Returns:
				tuple of (seed_vector, confidence_score)
		"""
		mood_text = MOOD_DESCRIPTIONS.get(mood, mood.lower())
		company_text = COMPANY_DESCRIPTIONS.get(company, company.lower())
		commitment_text = COMMITMENT_DESCRIPTIONS.get(commitment, commitment.lower())

		# Build tired_of modifier — reduces weight toward these regions
		tired_of_text = ""
		if tired_of:
			tired_of_text = " NOT " + " ".join(t.lower().replace("_", " ") for t in tired_of)

		# Embed each component separately
		mood_vector = self._embed(mood_text + tired_of_text)
		company_vector = self._embed(company_text)
		commitment_vector = self._embed(commitment_text)

		# Weighted average
		seed_vector = self._weighted_average([
			(mood_vector, MOOD_WEIGHT),
			(company_vector, COMPANY_WEIGHT),
			(commitment_vector, COMMITMENT_WEIGHT),
		])

		# Confidence — higher when mood is specific (not a generic selection)
		confidence = self._compute_confidence(mood, tired_of)

		logger.info(
			"[OnboardingService] Seed vector computed | mood=%s confidence=%.2f",
			mood, confidence
		)

		return seed_vector, confidence

	def _embed(self, text: str) -> List[float]:
		"""Embeds a single text string into a vector."""
		result = self.embedding_function([text])
		return result[0]

	def _weighted_average(self, vectors_and_weights: List[tuple[List[float], float]]) -> List[float]:
		"""
		Combines multiple vectors using weighted averaging.
		Normalizes weights to sum to 1.0.
		"""
		total_weight = sum(w for _, w in vectors_and_weights)
		dimension = len(vectors_and_weights[0][0])
		result = [0.0] * dimension

		for vector, weight in vectors_and_weights:
			normalized_weight = weight / total_weight
			for i in range(dimension):
				result[i] += vector[i] * normalized_weight

		return result

	def _compute_confidence(self, mood: str, tired_of: List[str]) -> float:
		"""
		Computes a confidence score for the seed vector.

		Specific moods get higher confidence. Tired-of selections
		add specificity and slightly increase confidence.
		"""
		# Base confidence per mood — specific moods are more confident
		mood_confidence = {
			"NEED_TO_FEEL": 0.80,
			"EDGE_OF_SEAT": 0.80,
			"JUST_LAUGH": 0.70,
			"MIND_BLOWN": 0.85,
			"QUIET_BEAUTIFUL": 0.85,
			"PURE_CHAOS": 0.75,
			"LEARN_SOMETHING": 0.80,
		}

		base = mood_confidence.get(mood, 0.65)

		# Each tired_of selection adds a small boost — more specific
		tired_of_boost = min(len(tired_of) * 0.03, 0.10)

		return round(min(base + tired_of_boost, 1.0), 2)
