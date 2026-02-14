import logging
from pathlib import Path
from typing import List, Optional

import chromadb
from chromadb import ClientAPI, QueryResult
from chromadb.api.models.Collection import Collection
from chromadb.utils import embedding_functions

from app.config.core import chroma_config
from app.dto.search_result import SearchResultItem
from app.engine.filter_builder import QueryFilterBuilder
from app.engine.mood_extractor import MoodExtractor
from app.engine.query_classifier import QueryType, QueryClassifier
from app.services.llm_service import LLMService

logger = logging.getLogger(__name__)


class QueryService:
	"""
  Orchestrates the transformation of natural language into structured
  ChromaDB queries using intent classification and entity extraction.
  """

	def __init__(self,
	             persist_directory: Path = chroma_config.persist_dir,
	             collection_name: str = chroma_config.collection_name) -> None:
		logger.info("[QueryService] Initializing ChromaDB")
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
		logger.info("[QueryService] Initialized ChromaDB (lazy)")

		# LLM clients and Query pipeline components
		logger.info("[QueryService] Initializing LLM service, and query pipeline components")
		self.llm = LLMService()
		self.classifier = QueryClassifier()
		self.mood_extractor = MoodExtractor()
		self.filter_builder = QueryFilterBuilder(llm_service=self.llm)

	def _initialize(self) -> None:
		"""Connects to the persistent storage and gets the specific collection."""
		if self.client is not None:
			return

		logger.info(
			"[QueryService] Initializing local ChromaDB at %s",
			self.persist_directory,
		)
		self.persist_directory.mkdir(parents=True, exist_ok=True)
		self.client = chromadb.PersistentClient(
			path=self.persist_directory,
		)
		self.collection = self.client.get_collection(
			name=self.collection_name,
			embedding_function=self.embedding_function,
		)
		logger.info(
			"[QueryService] Collection ready: %s",
			self.collection_name,
		)

	def search_batch(self,
	                 queries: List[str],
	                 top_k: int = 10) -> List[List[SearchResultItem]]:
		"""
		Executes multiple searches in a single batch.
		"""
		if self.collection is None:
			self._initialize()

		logger.info("[QueryService] Processing batch of %d queries", len(queries))

		try:
			results = self.collection.query(
				query_texts=queries,
				n_results=top_k,
			)
			return self._map_batch_results(results)
		except Exception as e:
			logger.error("[QueryService] Batch Chroma query failed: %s", e)
			return [[] for _ in queries]

	def _map_batch_results(self, results: QueryResult) -> List[List[SearchResultItem]]:
		"""
		Specially handles the nested structure of ChromaDB batch results.
		Returns a List of Lists (one list of movies per query).
		"""
		# Safety check: if Chroma returned nothing at all
		if not results.get("ids"):
			return []

		batch_output: List[List[SearchResultItem]] = []

		# In a batch result, results["ids"] is List[List[str]]
		# We iterate through each query's result set
		for i in range(len(results["ids"])):
			query_results: List[SearchResultItem] = []

			# Check if this specific query has any matches
			if not results["ids"][i]:
				batch_output.append([])
				continue

			# Slice the metadatas and documents for this specific query
			metadatas = results["metadatas"][i]
			documents = results["documents"][i]

			for j in range(len(metadatas)):
				meta = metadatas[j]
				doc = documents[j]

				query_results.append(
					SearchResultItem(
						content_id=int(meta.get("tmdb_id", 0)),
						content_type=meta.get("content_type", "movie"),
						title=meta.get("title", "Unknown"),
						overview=doc,
						release_year=meta.get("release_year"),
						language=meta.get("language"),
						poster_path=meta.get("poster_path"),
						analysis=meta.get("analysis", "Unknown"),
					)
				)
			batch_output.append(query_results)

		return batch_output

	def search(self, query: str, top_k: int = 10) -> List[SearchResultItem]:
		"""Process query and return top K results from ChromaDB."""
		if self.collection is None:
			self._initialize()

		logger.info(f"[QueryService] Processing query: {query}")

		query_type: QueryType = self.classifier.classify(query)
		mood_labels: List[str] = self.mood_extractor.extract(query)
		where_clause = None

		if query_type == QueryType.FACTUAL:
			where_clause = self.filter_builder.build_filters(query)
		if mood_labels:
			query = f"{query} (Moods: {', '.join(mood_labels)})"

		logger.info(f"[QueryService] Query Analysis: {query_type}, {mood_labels}, {where_clause}")

		try:
			results = self.collection.query(
				query_texts=[query],
				n_results=top_k,
				where=where_clause,
			)
		except Exception as e:
			logger.error(f"[QueryService] Chroma query failed: {e}")
			results = self.collection.query(query_texts=[query], n_results=top_k)

		# fallback to semantic search if we still don't have any results, as we want to show empty results
		results_none = results is None or len(results) == 0
		results_empty = results is not None and (len(results.get("ids")) == 0 or len(results["ids"][0]) == 0)
		if results_none or results_empty:
			logger.info("[QueryService] Fallback to semantic search")
			results = self.collection.query(query_texts=[query], n_results=top_k)

		return self._map_results(results, top_k)

	def _map_results(self, results: QueryResult, top_k: int) -> List[SearchResultItem]:
		if not results.get("ids") or not results["ids"][0]:
			logger.info("[QueryService] No results returned from database.")
			return []

		search_results: List[SearchResultItem] = []
		for metadata, doc in zip(results["metadatas"][0], results["documents"][0]):
			search_results.append(
				SearchResultItem(
					content_id=int(metadata.get("tmdb_id", 0)),
					content_type=metadata.get("content_type", "movie"),
					title=metadata.get("title", "Unknown"),
					overview=doc,
					release_year=metadata.get("release_year"),
					language=metadata.get("language"),
					poster_path=metadata.get("poster_path"),
					analysis=metadata.get("analysis"),
				)
			)
			if len(search_results) >= top_k:
				break

		logger.info("[QueryService] Returning %d SearchResultItems", len(search_results))
		return search_results
