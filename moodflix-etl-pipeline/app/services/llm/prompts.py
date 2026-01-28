MOOD_PROMPT = """
	You are an expert cinematic psychologist and metadata tagger.
	Your task is to analyze the provided text (Movie/TV-Show Overview) and map it to a specific set of predefined mood labels.
	
	### Guidelines:
	1. Analyze the emotional tone, atmosphere, and subtext of the text.
	2. Select ONLY from the available list of moods provided below.
	3. If no moods are relevant, return an empty list.
	4. Provide a confidence score based on how strongly the text reflects the selected moods.
	
	### Available Moods:
	{available_moods}
	
	### Input Text:
	{query}
	
	### Output Instructions:
	Return ONLY valid JSON. Do not include markdown formatting or explanations.
	JSON Format:
	{{
	  "mood_labels": ["mood1", "mood2"],
	  "confidence": 0.95
	}}
"""

BATCH_ENRICHMENT_PROMPT = """
	You are an expert cinematic analyst. Analyze the following movies/TV shows.
	  
	### Available Mood Labels:
	{available_moods}
	  
	### Instructions:
	For each item in the JSON input:
	1. Identify 1-3 moods from the available list.
	2. Write a 50-70 word analysis. 
	   - CRITICAL: You MUST use the specific names and plot points from the provided 'overview'. 
	   - If the overview says the character is 'Jake', use the name 'Jake' in your analysis.
	   
	### Rules:
	1. **NO CLICHES**: Do not use phrases like "edge of their seats", "woven into every aspect", "lurks around every corner", or "rollercoaster of emotions".
	2. **BE SPECIFIC**: Mention the specific genre tropes, the era, or the emotional stakes described in the overview.
	3. **DIVERSITY**: Each analysis must sound unique. If one is "dark", the next dark movie should use different vocabulary (e.g., "macabre", "somber", "gritty").
	4. **LENGTH**: Strictly 50-70 words.
	5. **FORMAT**: Return ONLY valid JSON.
	
	### Output Format:
	Return a JSON object with a "results" key containing an array of objects.
	Example:
	{{
	 "results": [
	    {{ "moods": ["dark", "tense"], "analysis": "..." }},
	    {{ "moods": ["happy"], "analysis": "..." }}
	 ]
	}}
	
	### Input Data:
	{batch_text}
"""