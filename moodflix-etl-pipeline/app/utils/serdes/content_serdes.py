from app.dto.models import ContentPackage
from app.utils.serdes.serdes_protocol import SerDesProtocol

class ContentSerDes(SerDesProtocol):
	"""Handles serialization and deserialization for LangChain Document objects."""

	# Type Hinting for clarity, ensuring the type T is Document
	T = ContentPackage

	def serialize(self, item: ContentPackage) -> str:
		"""Serializes a Document to a JSON string."""
		return item.model_dump_json()

	def deserialize(self, payload: bytes) -> ContentPackage:
		"""Deserializes a JSON string back to a Document object."""
		json_str = payload.decode("utf-8")
		return ContentPackage.model_validate_json(json_str)