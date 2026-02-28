import logging
from typing import List

from fastapi import APIRouter, Header
from pydantic import BaseModel

from app.services.service_registry import service_registry

logger = logging.getLogger(__name__)

router = APIRouter()


class MoodSelectionRequest(BaseModel):
	"""Mood-based onboarding selections from the gateway."""
	mood: str
	company: str
	commitment: str
	tiredOf: List[str] = []


class SeedVectorResponse(BaseModel):
	"""Seed vector response returned to the gateway."""
	seedVector: List[float]
	confidence: float


@router.post("/seed-vector", response_model=SeedVectorResponse)
def compute_seed_vector(request: MoodSelectionRequest, x_username: str = Header(alias="X-Username")) -> SeedVectorResponse:
	"""
	Computes a seed taste vector from mood selections.

	Called by the gateway after the user completes the onboarding
	mood quiz. Returns a float[384] vector that seeds the user's
	taste profile for personalized recommendations.

	The X-Username header is injected by the Spring gateway after
	JWT validation — never sent directly by the frontend.
	"""
	logger.info("[Onboarding] Computing seed vector | user=%s mood=%s",x_username, request.mood)

	seed_vector, confidence = service_registry.onboarding.compute_seed_vector(
		mood=request.mood,
		company=request.company,
		commitment=request.commitment,
		tired_of=request.tiredOf,
	)

	logger.info("[Onboarding] Seed vector computed | user=%s confidence=%.2f",
	            x_username, confidence)

	return SeedVectorResponse(
		seedVector=seed_vector,
		confidence=confidence,
	)
