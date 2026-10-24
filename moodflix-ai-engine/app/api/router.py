from fastapi import APIRouter
from app.api import search, onboarding, discover, movie

router = APIRouter(prefix="/api")
router.include_router(search.router, tags=["search"])
router.include_router(onboarding.router, tags=["onboarding"])
router.include_router(discover.router, tags=["discover"])
router.include_router(movie.router)
