from sentence_transformers import SentenceTransformer, util

from app.config.models import query_ml_config


class MoodExtractor:
	def __init__(self):
		self.model = SentenceTransformer(query_ml_config.mood_extractor_model_name)
		self.moods = [
			"happy", "sad", "lazy", "angry", "cozy", "excited",
			"tense", "scary", "romantic", "thought-provoking", "dark", "epic"
		]
		self.mood_embeddings = self.model.encode(self.moods, convert_to_tensor=True)
		self.threshold = 0.5

	def extract(self, query: str) -> list[str]:
		query_embedding = self.model.encode(query, convert_to_tensor=True)
		cosine_scores = util.cos_sim(query_embedding, self.mood_embeddings)[0]
		scores = cosine_scores[0].cpu().flatten().tolist()
		moods = [
			self.moods[i] for i, score in enumerate(scores)
			if score >= self.threshold
		]
		return moods