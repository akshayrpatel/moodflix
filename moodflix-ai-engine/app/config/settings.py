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
	app_name: str = 'moodflix-ai-engine'

	# classfier and mood extractor
	classifier_model_name: str
	mood_extractor_model_name: str

	# Ollama
	ollama_base_url: str
	ollama_gemma2_model_name: str
	ollama_gemma3_model_name: str

	# Mistral
	mistral_api_key: str
	mistral_model_name: str
	mistral_model_embed_name: str

	# Groq
	groq_api_key: str
	groq_model_name: str

	# Openrouter
	openrouter_api_key: str
	openrouter_model_name: str
	openrouter_base_url: str

	# embed
	embed_model_name: str

	# db
	db_root_dir: Path = APP_ROOT / 'db'

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