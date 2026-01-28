INSERT_CONTENT_BATCH_QUERY = """
  INSERT INTO content (
      tmdb_id,
      content_type,
      title,
      overview,
      release_date,
      release_year,
      language,
      popularity,
      vote_average,
      vote_count,
      genres,
      "cast",
      director,
      poster_path
  )
  VALUES (
      :tmdb_id,
      :content_type,
      :title,
      :overview,
      :release_date,
      :release_year,
      :language,
      :popularity,
      :vote_average,
      :vote_count,
      :genres,
      :cast,
      :director,
      :poster_path
  )
  ON CONFLICT (tmdb_id, content_type)
  DO UPDATE SET
      popularity = EXCLUDED.popularity,
      vote_average = EXCLUDED.vote_average,
      vote_count = EXCLUDED.vote_count,
      updated_at = NOW();
"""

INSERT_CHECKPOINT_QUERY = """
  INSERT INTO ingestion_checkpoints (
      content_type,
      last_page,
      last_fetched_at
  )
  VALUES (
      :content_type,
      :last_page,
      NOW()
  )
  ON CONFLICT (content_type)
  DO UPDATE SET
      last_page = EXCLUDED.last_page,
      last_fetched_at = NOW();
"""

GET_CHECKPOINT_QUERY = """
  SELECT last_page
  FROM ingestion_checkpoints
  WHERE content_type = :content_type;
"""