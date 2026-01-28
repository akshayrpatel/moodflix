import json
import logging

from typing import Dict, Any, Type, TypeVar
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import PromptTemplate
from langchain_ollama import OllamaLLM
from pydantic import BaseModel

from app.config.settings import settings

logger = logging.getLogger(__name__)

T = TypeVar("T", bound=BaseModel)

class LLMService:

	def __init__(self,
	             model: str = settings.ollama_model_name,
	             base_url: str = settings.ollama_base_url,
	             temperature: float = 0.0):
		self.llm = OllamaLLM(
			model=model,
			temperature=temperature,
			base_url=base_url,
			num_predict=4096,
			format="json"
		)

	def run(self,
	        prompt_template: PromptTemplate,
	        input_vars: Dict[str, Any],
	        output_schema: Type[T]) -> T:
		try:
			parser = JsonOutputParser(pydantic_object=output_schema)
			input_vars["format_instructions"] = parser.get_format_instructions()

			raw_response = (prompt_template | self.llm).invoke(input_vars)
			data = json.loads(raw_response)
			return output_schema(**data)
		except Exception as e:
			logger.error("[LLMService] Failed to process LLM output: %s", e)
			return output_schema()