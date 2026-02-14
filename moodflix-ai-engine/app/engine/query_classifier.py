import re
from enum import Enum
from transformers import pipeline

from app.config.models import query_ml_config

METADATA_PATTERNS = {
	"release_year": r"\b(19|20)\d{2}\b|\b(recent|new|old|classic|90s|80s|2000s)\b",
	"popularity": r"\b(popular|top|rated|famous|trending|best|hits|acclaimed)\b",
	"content_type": r"\b(movie|film|show|series|tv|documentary|episode|season)\b",
	"language": r"\b(english|spanish|french|korean|japanese|german|hindi|chinese|subtitled|dubbed)\b"
}

class QueryType(Enum):
	# Format: (type_id, description)
	FACTUAL = ("factual", "a search for specific movie years, languages, popularity, or types")
	SEMANTIC = ("semantic", "a request for a general mood, feeling, or vibe")

	def __init__(self, type_id, description):
		self.type_id = type_id
		self.description = description


class QueryClassifier:
	"""Detects if a query needs factual filtering."""

	def __init__(self):
		# Using a fast model for binary classification
		classifier_model = query_ml_config.classifier_model_name
		self._ml_classifier = pipeline(
			"zero-shot-classification",
			model=classifier_model,
			device=-1  # CPU
		)

	def classify(self, query: str) -> QueryType:
		# 1. Regex Check
		for category, pattern in METADATA_PATTERNS.items():
			if re.search(pattern, query, re.IGNORECASE):
				return QueryType.FACTUAL

		# 2. ML Fallback
		labels = [m.description for m in QueryType]
		result = self._ml_classifier(
			query,
			candidate_labels=labels,
			multi_label=False
		)

		predicted_label = result['labels'][0]
		predicted_score = result['scores'][0]

		if predicted_score > 0.5:
			for member in QueryType:
				if member.description == predicted_label:
					return member

		return QueryType.SEMANTIC
