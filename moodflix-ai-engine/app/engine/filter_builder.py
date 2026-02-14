import logging

from typing import Dict, Any
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import PromptTemplate

from app.engine.prompts import FILTER_BUILDER_PROMPT
from app.services.llm_service import LLMService

logger = logging.getLogger(__name__)


class QueryFilterBuilder:
	"""
	Extract and build structured metadata filters suitable for ChromaDB.
	"""

	def __init__(self, llm_service: LLMService):
		self.llm_service = llm_service

	def build_filters(self, query: str) -> Dict[str, Any] | None:
		logger.info("[QueryFilterBuilder] Building filters for query")
		parser = JsonOutputParser()
		prompt = PromptTemplate(template=FILTER_BUILDER_PROMPT,
		                        input_variables=["query"],
		                        partial_variables={"format_instructions": parser.get_format_instructions()})
		return self.llm_service.run(prompt_template=prompt,
			                           input_vars={"query": query},
			                           output_parser=parser)
