from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.config.core import postgres_config

engine = create_engine(
    postgres_config.db_url,
    pool_size=5,
    max_overflow=10,
    pool_pre_ping=True,
)

SessionLocal = sessionmaker(
    bind=engine,
    autoflush=False,
    autocommit=False,
)
