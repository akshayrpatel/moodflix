import logging
from typing import List
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.dto.models import ContentPackage, ContentItemType, ContentItem
from app.services.store.postgres_queries import INSERT_CONTENT_BATCH_QUERY, INSERT_CHECKPOINT_QUERY, \
	GET_CHECKPOINT_QUERY

logger = logging.getLogger(__name__)


class PostgresService:
	def __init__(self, session_factory):
		self.SessionLocal = session_factory

	def write_batch(self, session: Session, items: List[ContentPackage]) -> None:
		if not items:
			return

		for item in items:
			content_type: ContentItemType = item.content_type
			content_items: List[ContentItem] = item.content_items
			page: int = item.page
			payloads = [
				{
					"tmdb_id": item.id,
					"content_type": item.content_type,
					"title": item.title,
					"overview": item.overview,
					"release_date": item.release_date,
					"release_year": item.release_year,
					"language": item.language,
					"popularity": item.popularity,
					"vote_average": item.vote_average,
					"vote_count": item.vote_count,
					"genres": item.genres,
					"cast": item.cast,
					"director": item.director,
					"poster_path": item.poster_path,
				}
				for item in content_items
			]

			self._save_batch(session, payloads)
			self._save_checkpoint(session, content_type, page)
			logger.info(f"[PostgresWriter] Prepared batch for type:{content_type}, page:{page}")

	def _save_batch(self, session, batch):
		query = text(INSERT_CONTENT_BATCH_QUERY)
		session.execute(query, batch)

	def _save_checkpoint(self, session, content_type: str, last_page: int) -> None:
		query = text(INSERT_CHECKPOINT_QUERY)
		data = {"content_type": content_type, "last_page": last_page}
		session.execute(query, data)

	def get_checkpoint(self, content_type: str) -> int:
		query = text(GET_CHECKPOINT_QUERY)
		data = {"content_type": content_type}
		with self.SessionLocal() as session:
			result = session.execute(query, data)
			row = result.fetchone()
			return row[0] if row else 0