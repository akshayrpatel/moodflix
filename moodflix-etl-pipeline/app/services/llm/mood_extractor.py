import logging
import json

from typing import List, Any
from pydantic import BaseModel, Field
from langchain_core.prompts import PromptTemplate

from app.dto.models import ContentItem
from app.services.llm.llm_service import LLMService
from app.services.llm.prompts import MOOD_PROMPT, BATCH_ENRICHMENT_PROMPT

logger = logging.getLogger(__name__)


class MoodSchema(BaseModel):
	mood_labels: List[str] = Field(default_factory=list)
	confidence: float = Field(default=1.0)

class ContentAnalysis(BaseModel):
	moods: List[str]
	analysis: str

class BatchMoodSchema(BaseModel):
	results: List[ContentAnalysis] = Field(default_factory=list)


class MoodExtractor:
	def __init__(self, llm_client: LLMService, available_moods: List[str]):
		self.llm_client = llm_client
		self.available_moods = available_moods or [
			"happy", "sad", "lazy", "angry", "cozy", "excited",
			"tense", "scary", "romantic", "thought-provoking", "dark", "epic"
		]

	def extract(self, text: str) -> MoodSchema:
		logger.info("[QueryMoodExtractor] Extracting mood for query: %s", text[:10])
		prompt_template = PromptTemplate(
			template=MOOD_PROMPT,
			input_variables=["text", "available_moods"]
		)
		try:
			result: MoodSchema = self.llm_client.run(
				prompt_template=prompt_template,
				input_vars={
					"query": text,
					"available_moods": ", ".join(self.available_moods)
				},
				output_schema=MoodSchema
			)

			# Post-processing: Ensure LLM only returned valid moods from our list
			valid_labels = [
				m for m in result.mood_labels
				if m.lower().strip() in self.available_moods
			]

			return MoodSchema(mood_labels=valid_labels, confidence=result.confidence)

		except Exception as e:
			logger.error(f"[MoodExtractor] Extraction failed: {e}")
			return MoodSchema(mood_labels=[], confidence=0.0)

	def extract_batch(self, items: List[ContentItem]) -> List[ContentAnalysis]:
		if not items:
			return []

		# Process in chunks of 5 to avoid LLM output truncation
		chunk_size = 5
		all_results = []
		for start in range(0, len(items), chunk_size):
			chunk = items[start:start + chunk_size]
			result = self._extract_chunk(chunk)
			all_results.extend(result)
		return all_results

	def _extract_chunk(self, items: List[ContentItem]) -> List[ContentAnalysis]:
		batch_json = [
			{"id": i, "title": item.title, "overview": item.overview}
			for i, item in enumerate(items)
		]

		max_retries = 3
		for attempt in range(max_retries):
			try:
				prompt = PromptTemplate(
					template=BATCH_ENRICHMENT_PROMPT,
					input_variables=["batch_text", "available_moods"]
				)
				response: BatchMoodSchema = self.llm_client.run(
					prompt_template=prompt,
					input_vars={
						"batch_text": json.dumps(batch_json),
						"available_moods": ", ".join(self.available_moods)
					},
					output_schema=BatchMoodSchema
				)

				print(f"\n[DEBUG] LLM given {len(batch_json)} input was {batch_json}")
				print(f"[DEBUG] LLM returned {len(response.results)} results for {len(items)} items")
				print(f"[DEBUG] All results: {response.results if response.results else 'EMPTY'}")

				if len(response.results) != len(items):
					logger.warning(f"[MoodExtractor] Result count mismatch: expected {len(items)}, got {len(response.results)} (attempt {attempt + 1}/{max_retries})")
					continue

				batch_movie_enrichment = []
				for res in response.results:
					valid_moods = [
						m.lower().strip() for m in res.moods
						if m.lower().strip() in self.available_moods
					]
					if not valid_moods or not res.analysis.strip():
						logger.warning(f"[MoodExtractor] Incomplete result: moods={res.moods}, analysis='{res.analysis}' (attempt {attempt + 1}/{max_retries})")
						break
					batch_movie_enrichment.append(ContentAnalysis(moods=valid_moods, analysis=res.analysis))
				else:
					logger.info(f"[BatchMoodExtractor] Finished creating moods, and analysis for {len(batch_movie_enrichment)} items")
					return batch_movie_enrichment

				continue

			except Exception as e:
				logger.error(f"[ContentMoodExtractor] Batch extraction failed (attempt {attempt + 1}/{max_retries}): {e}")
				continue

		logger.error(f"[MoodExtractor] All {max_retries} attempts failed for chunk of {len(items)} items")
		return [ContentAnalysis(moods=[], analysis="") for _ in items]
