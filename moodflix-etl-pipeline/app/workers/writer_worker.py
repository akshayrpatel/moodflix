import logging
import time
import threading
from typing import List

from app.config.core import pipeline_config
from app.dto.models import ContentPackage
from app.services.queue_service import RedisBufferQueue
from app.services.store.chroma_service import ChromaService
from app.services.store.postgres_service import PostgresService

logger = logging.getLogger(__name__)


class WriterWorker:
	"""
	Contract rules
	1. If it exists in Postgres → it should exist in Chroma
	2. If it exists in Chroma → it must exist in Postgres
	3. Temporary violations of (1) are allowed
	4. Violations of (2) are never allowed
	"""
	def __init__(self,
	             content_queue: RedisBufferQueue,
	             pg_service: PostgresService,
	             ch_service: ChromaService,
	             shutdown_event: threading.Event):
		self.queue = content_queue
		self.postgres_service = pg_service
		self.chroma_service = ch_service
		self.thread = threading.Thread(target=self._run, name="WriterWorker")
		self._stop_event = shutdown_event

	def start(self):
		self.thread.start()

	def stop(self):
		logger.warning("[WriterWorker] Shutdown signal received. Cleaning up...")
		self._stop_event.set()

	def _run(self):
		logger.info("[WriterWorker] Monitoring queue for new content...")

		while True:
			if self._stop_event.is_set() and self.queue.size() == 0:
				logger.info("[WriterWorker] Shutdown + queue drained. Exiting.")
				break

			batch: List[ContentPackage] = self.queue.pop_batch(batch_size=2)
			if not batch:
				logger.info(f"[WriterWorker] Queue empty. Cooling down for {pipeline_config.writer_interval}s")
				time.sleep(pipeline_config.writer_interval)
				continue

			with self.postgres_service.SessionLocal() as session:
				try:
					# self.postgres_service.write_batch(session, batch)
					self.chroma_service.write_batch(batch)
					session.commit()
				except Exception as e:
					session.rollback()
					logger.error(f"[WriterWorker] Postgres or Chroma write failed, rolling back: {e}", exc_info=True)
					continue

			logger.info(f"[WriterWorker] Batch complete. Cooling down for {pipeline_config.writer_interval}s")
			time.sleep(pipeline_config.writer_interval)

		logger.info("[WriterWorker] Ingestion cycle finished. Thread exiting.")
