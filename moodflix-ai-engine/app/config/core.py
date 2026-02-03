from pathlib import Path

from app.config.settings import settings

class ChromaConfig:
	embed_model_name = settings.embed_model_name
	persist_dir: Path = settings.db_root_dir / settings.chromadb_dir
	collection_name: str = settings.chromadb_collection_name

chroma_config = ChromaConfig()