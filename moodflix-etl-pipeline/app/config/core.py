from pathlib import Path

from app.config.settings import settings

class RedisConfig:
	max_retries: int = 3
	sleep_timer: int = 3

class ContentQueueConfig:
	queue_url: str = settings.content_queue_url
	queue_name: str = 'content_queue'

class PostgresConfig:
	db_url: str = f"postgresql://postgres:{settings.postgres_password}@{settings.postgres_host}:{settings.postgres_port}/{settings.postgres_database}"

class ChromaConfig:
	embed_model_name: str = settings.embed_model_name
	persist_dir: Path = settings.db_root_dir / settings.chromadb_dir
	collection_name: str = settings.chromadb_collection_name

class PipelineConfig:
	worker_retries: int = 3
	worker_delay: int = 10
	pages_to_fetch: int = 250
	page_interval: int = 5
	page_item_interval: int = 1
	writer_interval: int = 8
	queue_max_size: int = 500

redis_config = RedisConfig()
content_queue_config = ContentQueueConfig()
postgres_config = PostgresConfig()
chroma_config = ChromaConfig()
pipeline_config = PipelineConfig()