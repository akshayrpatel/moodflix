import logging

from typing import List, Dict, Any, TypeVar
from langchain_core.language_models import BaseChatModel
from langchain_core.output_parsers import BaseOutputParser
from langchain_core.prompts import PromptTemplate
from langchain_groq import ChatGroq
from langchain_mistralai import ChatMistralAI
from langchain_ollama import ChatOllama
from langchain_openai import ChatOpenAI

from app.config.models import mistral_config, groq_config, openrouter_config, ollama_config

logger = logging.getLogger(__name__)

T = TypeVar("T", bound=BaseOutputParser)

class LLMService:
	"""
	Service for querying multiple LLM providers with automatic failover.

  This service maintains a list of LLM providers, and attempts to
  query them in order until one successfully returns a response.

  Attributes:
    providers (List[BaseChatModel]): Ordered list of LLM providers. The
        service iterates through these providers in sequence and falls back
        to the next one if a provider fails.
  """

	def __init__(self) -> None:
		self.providers: List[BaseChatModel] = [
			ChatGroq(
				api_key=groq_config.api_key,
				model=groq_config.model,
				temperature=groq_config.temperature,
				max_retries=groq_config.max_retries,
			),
			ChatMistralAI(
				api_key=mistral_config.api_key,
				model_name=mistral_config.model,
				temperature=mistral_config.temperature,
				max_retries=mistral_config.max_retries
			),
			ChatOpenAI(
				api_key=openrouter_config.api_key,
				model=openrouter_config.model,
				base_url=openrouter_config.base_url,
				temperature=openrouter_config.temperature,
			),
			ChatOllama(
				model=ollama_config.model_qwen25,
				temperature=0,
				base_url=ollama_config.base_url,
				format="json",
			),
			ChatOllama(
				model=ollama_config.model_phi3_mini,
				temperature=0,
				base_url=ollama_config.base_url,
				format="json",
			),
			ChatOllama(
				model=ollama_config.model_gemma2,
				temperature=ollama_config.temperature,
				base_url=ollama_config.base_url
			),
			ChatOllama(
				model=ollama_config.model_gemma3,
				temperature=ollama_config.temperature,
				base_url=ollama_config.base_url
			),
		]

	def get_failover_provider(self):
		"""
		Returns a single Runnable that automatically tries the next provider if the current one fails.
		"""
		primary = self.providers[0]
		return primary.with_fallbacks(fallbacks=self.providers[1:])

	def run(self,
	        prompt_template: PromptTemplate,
	        input_vars: Dict[str, Any],
	        output_parser: BaseOutputParser[T]) -> T | None:
		for provider in self.providers:
			try:
				model_name = getattr(provider, "model", getattr(provider, "model_name", "Unknown Model"))
				provider_type = type(provider).__name__

				chain = prompt_template | provider | output_parser
				response = chain.invoke(input_vars)

        logger.info(f"[LLMService] Provider {type(provider).__name__} succeeded")
				return response
			except Exception as e:
				logger.warning(f"[LLMService] Provider {type(provider).__name__} failed: {e}", exc_info=e)
				continue

		logger.error("[LLMService] All llm providers failed to execute the chain.")
		return output_parser