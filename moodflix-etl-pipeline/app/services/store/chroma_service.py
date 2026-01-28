import json

import chromadb
import logging
from pathlib import Path
from typing import List, Optional
from chromadb.api import ClientAPI
from chromadb.api.models.Collection import Collection
from chromadb.utils import embedding_functions

from app.config.core import chroma_config
from app.dto.models import ContentItem, ContentPackage
from app.services.llm.mood_extractor import MoodExtractor, ContentAnalysis

logger = logging.getLogger(__name__)


class ChromaService:
	"""
	Writes ContentItem embeddings into ChromaDB.
	Chroma is a derived index and can be safely rebuilt.
	"""

	def __init__(self,
	             mood_extractor: MoodExtractor,
	             persist_directory: Path = chroma_config.persist_dir,
	             collection_name: str = chroma_config.collection_name) -> None:
		self.persist_directory = persist_directory
		self.collection_name = collection_name

		self.client: Optional[ClientAPI] = None
		self.collection: Optional[Collection] = None

		self.embedding_function = (
			embedding_functions.SentenceTransformerEmbeddingFunction(
				model_name=chroma_config.embed_model_name,
				normalize_embeddings=True,
			)
		)
		self.mood_extractor = mood_extractor

	def _initialize(self) -> None:
		if self.client is not None:
			return

		logger.info(
			"[ChromaWriter] Initializing local ChromaDB at %s",
			self.persist_directory,
		)

		self.persist_directory.mkdir(parents=True, exist_ok=True)

		self.client = chromadb.PersistentClient(
			path=self.persist_directory,
		)

		self.collection = self.client.get_or_create_collection(
			name=self.collection_name,
			embedding_function=self.embedding_function,
		)

		logger.info(
			"[ChromaWriter] Collection ready: %s",
			self.collection_name,
		)

	def write_batch(self, items: List[ContentPackage]) -> None:
		if not items:
			return

		if self.client is None:
			self._initialize()

		for package in items:
			content_type = package.content_type
			items = package.content_items
			page = package.page

			logger.info("[ChromaWriter] Enriching batch with LLM-generated moods and analysis...")
			try:
				enriched_data: List[ContentAnalysis] = self.mood_extractor.extract_batch(items)
				ids = []
				documents = []
				metadatas = []

				for item, enrichment in zip(items, enriched_data):
					moods_str = ",".join(enrichment.moods)
					analysis = enrichment.analysis
					doc_text = (
						f"Title: {item.title}. "
						f"Overview: {item.overview}. "
						f"Director: {item.director}. "
						f"Cast: {",".join([c.replace(" ", "").lower() for c in item.cast])}. "
						f"Genres: {",".join([g.replace(" ", "").lower() for g in item.genres])}. "
						f"Moods: {moods_str}. "
						f"Analysis: {analysis}"
					)

					ids.append(item.chroma_id)
					documents.append(doc_text)
					metadatas.append({
						"tmdb_id": item.id,
						"content_type": item.content_type.value,
						"title": item.title,
						"release_year": item.release_year,
						"language": item.language,
						"popularity": item.popularity,
						"genres": ",".join([g.replace(" ", "").lower() for g in item.genres]),
						"cast": ",".join([c.replace(" ", "").lower() for c in item.cast]),
						"director": item.director or "",
						"poster_path": item.poster_path or "",
						"moods": moods_str,
						"analysis": analysis
					})

				self.collection.upsert(
					ids=ids,
					documents=documents,
					metadatas=metadatas,
				)
				logger.info(f"[ChromaWriter] Successfully synced {len(items)}, for type:{content_type}, page:{page}")

			except Exception as e:
				logger.error(f"[ChromaWriter] Failed to write, for type:{content_type}, page:{page} - {e}", exc_info=True)
				raise
