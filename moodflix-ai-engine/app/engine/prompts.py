CLASSIFICATION_PROMPT = """
Analyze the query: "{query}"
Detect which of the following signals are present:
- SEMANTIC: Abstract vibes, emotions, or themes (e.g., "sad movies").
- FACTUAL: Database attributes like years, language, or content type (e.g., "movies from 2022").

{format_instructions}
"""

MOOD_EXTRACTION_PROMPT = """
Extract relevant mood labels from the query: "{query}"
ONLY use labels from this list: {available_moods}
If no labels match, return an empty list.

{format_instructions}
"""

FILTER_BUILDER_PROMPT = """
You are a specialized Query Optimizer that converts natural language into a structured JSON Filter Schema for a ChromaDB vector database.

### METADATA SCHEMA:
- `release_year` (int): Year of release.
- `content_type` (string): Exactly "movie" or "tv_show".
- `language` (string): ISO 639-1 code (e.g., "en", "es", "ja", "ko").
- `popularity` (float): Score 0-100.

### CRITICAL SEARCH VS FILTER RULE:
- Entities like Genres (Action, Horror), Cast (Tom Cruise), Directors (Nolan), and Moods (Scary, Sad) are NOT metadata fields.
- DO NOT include them in this JSON. They are handled by the separate semantic vector layer.
- If a query ONLY contains semantic vibes (e.g., "funny cat videos"), return an empty object: {{}}

### OPERATOR DECISION MATRIX:
1. SPECIFIC YEAR (e.g., "in 2022"): {{ "release_year": {{ "$eq": 2022 }} }}
2. AFTER/NEWER (e.g., "since 2020"): {{ "release_year": {{ "$gte": 2020 }} }}
3. BEFORE/OLDER (e.g., "pre-1990"): {{ "release_year": {{ "$lte": 1990 }} }}
4. DECADES/RANGES (e.g., "90s"): Use "$and" with {{ "$gte": 1990 }} and {{ "$lte": 1999 }}.

### CHROMA BINDING RULES (STRICT):
1. Every filter must be a single root object.
2. If the filter involves MORE THAN ONE field (e.g., language AND year), you MUST use the "$and" operator as the only top-level key.
3. Example of valid multi-filter: {{ "$and": [ {{ "language": {{ "$eq": "en" }} }}, {{ "content_type": {{ "$eq": "movie" }} }} ] }}

### EXAMPLES:
- Query: "Classic 1940s films" -> {{ "$and": [ {{ "release_year": {{ "$gte": 1940 }} }}, {{ "release_year": {{ "$lte": 1949 }} }} ] }}
- Query: "TV shows in Spanish" -> {{ "$and": [ {{ "content_type": {{ "$eq": "tv_show" }} }}, {{ "language": {{ "$eq": "es" }} }} ] }}
- Query: "Highly rated documentaries after 2010" -> {{ "$and": [ {{ "popularity": {{ "$gte": 80 }} }}, {{ "release_year": {{ "$gt": 2010 }} }} ] }}
- Query: "Funny cat videos" -> {{}}

### OUTPUT INSTRUCTIONS:
{format_instructions}

Query: {query}
Output:
"""