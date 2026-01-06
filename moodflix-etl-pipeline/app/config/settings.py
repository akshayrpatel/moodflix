import os

from enum import Enum
from pathlib import Path
from pydantic_settings import BaseSettings

APP_ROOT = Path(__file__).parents[2]

class AppMode(str, Enum):
	DEVELOPMENT = "development"
	PRODUCTION = "production"

def get_env() -> str:
	return os.getenv("APP_ENV", AppMode.DEVELOPMENT.value)

def load_env_file() -> Path:
	return APP_ROOT / f".env.{get_env()}"

class Settings(BaseSettings):
	# environment
	app_root: Path = APP_ROOT
	app_env: str = get_env()
	app_name: str = 'moodflix-etl'

	# TMDB
	tmdb_api_key: str = None
	tmdb_api_read_token: str = None

	# Queue
	content_queue_url: str

	# Ollama
	ollama_model_name: str
	ollama_base_url: str

	# Embed
	embed_model_name: str

	# store
	db_root_dir: Path = APP_ROOT / 'store'

	# postgres
	postgres_user: str = None
	postgres_password: str = None
	postgres_host: str = None
	postgres_port: int = None
	postgres_database: str = None

	# vectordb
	chromadb_dir: str
	chromadb_host: str
	chromadb_port: int
	chromadb_collection_name: str

	model_config = {
		"env_file": load_env_file(),
		"env_file_encoding": "utf-8"
	}

settings = Settings()