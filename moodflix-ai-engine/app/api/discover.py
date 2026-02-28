from fastapi import APIRouter, Header, HTTPException
from typing import List, Optional

from app.dto.search_result import SearchResultItem
from app.services.service_registry import service_registry

router = APIRouter()


def _parse_vector(header_value: str) -> List[float]:
    """Parse comma-separated float string from X-User-Vector header."""
    try:
        cleaned = header_value.strip().strip("[]")
        return [float(x.strip()) for x in cleaned.split(",")]
    except (ValueError, AttributeError):
        raise HTTPException(status_code=400, detail="Invalid taste vector format")


@router.get("/discover")
def discover(x_user_vector: Optional[str] = Header(None, alias="X-User-Vector")):
    print("/discover")
    print(f"[DISCOVER] Header present: {x_user_vector is not None}")
    print(f"[DISCOVER] Header value (first 80 chars): {str(x_user_vector)[:80] if x_user_vector else 'None'}")

    if not x_user_vector:
        raise HTTPException(status_code=400, detail="Missing X-User-Vector header")

    taste_vector = _parse_vector(x_user_vector)
    print(f"[DISCOVER] Parsed vector length: {len(taste_vector)}")

    if len(taste_vector) != 384:
        raise HTTPException(
            status_code=400,
            detail=f"Expected 384-dim vector, got {len(taste_vector)}",
        )

    rows = service_registry.discover.discover(taste_vector)

    return {
        "rows": [
            {"key": "vibe", "title": "Your Vibe Tonight", "items": rows["your_vibe"]},
            {"key": "hidden", "title": "Hidden Gems For You", "items": rows["hidden_gems"]},
            {"key": "different", "title": "Something Different", "items": rows["something_different"]},
            {"key": "trending", "title": "Trending Now", "items": rows["trending_now"]},
        ]
    }
