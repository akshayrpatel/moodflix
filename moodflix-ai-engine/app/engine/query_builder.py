import logging

from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field
from typing import List, Dict, Any
from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.runnables import RunnableParallel

from app.engine.prompts import MOOD_EXTRACTION_PROMPT, FILTER_BUILDER_PROMPT, CLASSIFICATION_PROMPT
from app.services.llm_service import LLMService

logger = logging.getLogger(__name__)


class QueryClassificationSchema(BaseModel):
	has_semantic: bool = Field(description="True if query has abstract vibes")
	has_factual: bool = Field(description="True if query has metadata filters")
	explanation: str = Field(description="One sentence reasoning")

class MoodExtractionSchema(BaseModel):
	moods: List[str] = Field(default_factory=list, description="Labels from the whitelist")

class FilterBuilderSchema(BaseModel):
	filters: Dict[str, Any] = Field(default_factory=dict, description="ChromaDB JSON")

class UnifiedQueryPlan(BaseModel):
	has_semantic: bool = Field(default=False)
	has_factual: bool = Field(default=False)
	has_hybrid: bool = Field(default=False)
	explanation: str = Field(default="")
	mood_labels: List[str] = Field(default_factory=list)
	confidence: float = Field(default=1.0)
	filters: Dict[str, Any] = Field(
		default_factory=dict,
		description="Structured ChromaDB filters."
	)

class QueryBuilder:
	def __init__(self, llm_service: LLMService):
		self.llm = llm_service.get_failover_provider()
		self.moods = [
			"happy", "sad", "lazy", "angry", "cozy", "excited",
			"tense", "scary", "romantic", "thought-provoking", "dark", "epic"
		]

	async def process(self, query: str):
		classification_parser = PydanticOutputParser(pydantic_object=QueryClassificationSchema)
		mood_parser = PydanticOutputParser(pydantic_object=MoodExtractionSchema)
		filter_parser = PydanticOutputParser(pydantic_object=FilterBuilderSchema)

		classification_prompt = ChatPromptTemplate.from_template(CLASSIFICATION_PROMPT).partial(
        format_instructions=classification_parser.get_format_instructions()
    )
		mood_prompt = ChatPromptTemplate.from_template(MOOD_EXTRACTION_PROMPT).partial(
        format_instructions=mood_parser.get_format_instructions(),
        available_moods=", ".join(self.moods) # Also inject moods here to keep invoke clean
    )
		filter_prompt = ChatPromptTemplate.from_template(FILTER_BUILDER_PROMPT).partial(
        format_instructions=filter_parser.get_format_instructions()
    )
		chain = RunnableParallel({
			"classification": classification_prompt | self.llm | classification_parser,
			"moods": mood_prompt | self.llm | mood_parser,
			"filters": filter_prompt | self.llm | filter_parser,
		})

		raw = await chain.ainvoke({"query": query, "available_moods": self.moods})

		return UnifiedQueryPlan(
			has_semantic=raw["classification"].has_semantic,
			has_factual=raw["classification"].has_factual,
			mood_labels=raw["moods"].moods,
			filters=raw["filters"].filters,
			explanation=raw["classification"].explanation
		)
