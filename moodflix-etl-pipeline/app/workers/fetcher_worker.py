import threading
import logging
import time
from typing import List
from app.config.core import pipeline_config
from app.config.settings import settings
from app.dto.models import ContentItem, ContentPackage
from app.services.fetcher.movie_content_fetcher import MovieFetcher
from app.services.fetcher.tv_content_fetcher import TVFetcher
from app.services.queue_service import RedisBufferQueue
from app.services.store.postgres_service import PostgresService

logger = logging.getLogger(__name__)


class FetcherWorker:
	def __init__(self, content_queue: RedisBufferQueue, pg_service: PostgresService, shutdown_event: threading.Event):
		self.queue = content_queue
		self.postgres_service = pg_service
		self.movie_fetcher = MovieFetcher(settings.tmdb_api_key)
		self.tv_fetcher = TVFetcher(settings.tmdb_api_key)
		self.thread = threading.Thread(target=self._run, name="FetcherWorker")
		self._stop_event = shutdown_event

	def start(self):
		self.thread.start()

	def stop(self):
		logger.warning("[FetcherWorker] Stop signal received. Finishing current item...")
		self._stop_event.set()

	def _run(self):
		logger.info("[FetcherWorker] Starting TMDB ingestion.")

		fetchers = [self.movie_fetcher]
		for fetcher in fetchers:
			content_type = fetcher.get_type().value
			last_fetched_page = self.postgres_service.get_checkpoint(content_type)
			start_page = last_fetched_page + 1
			end_page = start_page + pipeline_config.pages_to_fetch

			for page in range(start_page, end_page + 1):

				#  handling interrupts
				if self._stop_event.is_set():
					logger.info(f"[FetcherWorker] Interrupted during type:{content_type}, page:{page}, exiting.")
					break

				#  backpressure management
				if self.queue.size() > pipeline_config.queue_max_size:
					logger.info(f"[FetcherWorker] Queue full, managing backpressure. Cooling down for {pipeline_config.page_interval}s")
					time.sleep(pipeline_config.page_interval)
					continue

				success = False
				for attempt in range(pipeline_config.worker_retries):

					logger.info(f"[FetcherWorker] Fetch type:{content_type}, page:{page} | range=[{start_page}-{end_page}]")
					try:
						raw_items = fetcher.fetch_page(page)
						content_items: List[ContentItem] = []

						for index, raw_item in enumerate(raw_items):
							detail_data = fetcher.fetch_details(content_id=raw_item["id"], path_prefix=content_type)
							content_items.append(fetcher.normalize(detail_data))
							time.sleep(pipeline_config.page_item_interval)

						if len(content_items) == 0:
							logger.warning(f"[FetcherWorker] No items fetched for type:{content_type}, page:{page}. Cooling down for {pipeline_config.page_interval}s")
							time.sleep(pipeline_config.page_interval)
							continue

						content_package = ContentPackage(content_type=content_type, content_items=content_items, page=page)
						self.queue.push_batch([content_package])

						success = True
						logger.info(f"[FetcherWorker] Successfully pushed type:{content_type}, page:{page}, a package of {len(content_items)} items. Cooling down for {pipeline_config.page_interval}s")
						time.sleep(pipeline_config.page_interval)
						break

					except Exception as e:
						backoff = (attempt + 1) * 5
						logger.error(f"[FetcherWorker] Attempt: {attempt}, failed to process type:{content_type}, page:{page}, backing off for {backoff}s: {str(e)}", exc_info=True)
						time.sleep(backoff)

				if not success:
					logger.critical(f"[FetcherWorker] Max retries reached for type:{content_type}, page:{page}. Skipping.")

		logger.info("[FetcherWorker] Ingestion cycle finished. Signaling shutdown.")
		self._stop_event.set()
