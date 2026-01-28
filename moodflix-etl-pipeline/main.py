import logging
import time
from threading import Event

from app.config.core import content_queue_config, pipeline_config, chroma_config
from app.config.logging_config import configure_logging
from app.services.llm.llm_service import LLMService
from app.services.llm.mood_extractor import MoodExtractor

from app.services.store.postgres_service import PostgresService
from app.services.store.chroma_service import ChromaService
from app.services.queue_service import RedisBufferQueue
from app.services.store.postgres_session import SessionLocal
from app.utils.serdes.content_serdes import ContentSerDes

from app.workers.fetcher_worker import FetcherWorker
from app.workers.writer_worker import WriterWorker

logger = logging.getLogger(__name__)


class Pipeline:
	def __init__(self,
	             content_queue: RedisBufferQueue,
	             postgres_service: PostgresService,
	             chroma_service: ChromaService,
	             shutdown_event: Event) -> None:

		logger.info("[Pipeline] Initializing Moodflix ETL...")
		self.fetcher_worker = FetcherWorker(content_queue, postgres_service, shutdown_event)
		self.writer_worker = WriterWorker(
			content_queue, postgres_service, chroma_service, shutdown_event
		)
		self.workers = [self.fetcher_worker, self.writer_worker]
		self.shutdown_event = shutdown_event

	def start(self) -> None:
		for worker in self.workers:
			worker.start()
			logger.info(f"[Pipeline] Started: {worker.__class__.__name__}")
			time.sleep(pipeline_config.worker_delay)

	def stop(self) -> None:
		logger.warning("[Pipeline] Stopping workers...")
		for worker in self.workers:
			worker.stop()
		for worker in self.workers:
			worker.thread.join()
		logger.info("[Pipeline] All workers stopped.")


if __name__ == "__main__":
	configure_logging()
	queue = RedisBufferQueue(
		redis_url=content_queue_config.queue_url,
		queue_name=content_queue_config.queue_name,
		serializer=ContentSerDes().serialize,
		deserializer=ContentSerDes().deserialize
	)
	queue.clear()

	pg_service = PostgresService(
		session_factory=SessionLocal
	)
	ch_service = ChromaService(
		mood_extractor=MoodExtractor(llm_client=LLMService(), available_moods=[]),
		persist_directory=chroma_config.persist_dir,
		collection_name=chroma_config.collection_name
	)

	pipeline_shutdown_event = Event()
	moodflix_etl = Pipeline(
		content_queue=queue,
		postgres_service=pg_service,
		chroma_service=ch_service,
		shutdown_event=pipeline_shutdown_event
	)

	try:
		moodflix_etl.start()
		for worker in moodflix_etl.workers:
			worker.thread.join()
	except KeyboardInterrupt:
		pipeline_shutdown_event.set()
		moodflix_etl.stop()

	logger.info("[Main] Pipeline complete.")