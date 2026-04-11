from pydantic import BaseModel
from app.config.settings import settings

class QueryMLConfig(BaseModel):
	classifier_model_name: str = settings.classifier_model_name
	mood_extractor_model_name: str = settings.mood_extractor_model_name

class OllamaConfig(BaseModel):
	model_gemma2: str = settings.ollama_gemma2_model_name
	model_gemma3: str = settings.ollama_gemma3_model_name
	model_phi3_mini: str = settings.ollama_phi3_mini_model_name
	model_qwen25: str = settings.ollama_qwen25_model_name
	base_url: str = settings.ollama_base_url
	temperature: float = 0.5
	max_retries: int = 2

class MistralConfig(BaseModel):
	api_key: str = settings.mistral_api_key
	model: str = settings.mistral_model_name
	model_embed: str = settings.mistral_model_embed_name
	temperature: float = 0.5
	max_retries: int = 2

class GroqConfig(BaseModel):
	api_key: str = settings.groq_api_key
	model: str = settings.groq_model_name
	temperature: float = 0.5
	max_retries: int = 2

class OpenRouterConfig(BaseModel):
	api_key: str = settings.openrouter_api_key
	model: str = settings.openrouter_model_name
	base_url: str = settings.openrouter_base_url
	temperature: float = 0.5
	max_retries: int = 2

query_ml_config = QueryMLConfig()
ollama_config = OllamaConfig()
mistral_config = MistralConfig()
groq_config = GroqConfig()
openrouter_config = OpenRouterConfig()
