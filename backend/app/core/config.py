"""Configuration settings."""
import json
import os
from dotenv import load_dotenv

load_dotenv()

SNOWFLAKE_CONFIG = {
    "user": os.getenv("SNOWFLAKE_USER"),
    "password": os.getenv("SNOWFLAKE_PASSWORD"),
    "account": os.getenv("SNOWFLAKE_ACCOUNT"),
    "database": os.getenv("SNOWFLAKE_DATABASE", "CADENCE"),
    "schema": os.getenv("SNOWFLAKE_SCHEMA", "PUBLIC"),
}

_raw_origins = os.getenv("ALLOWED_ORIGINS", "")
try:
    ALLOWED_ORIGINS: list[str] = json.loads(_raw_origins) if _raw_origins else []
except (json.JSONDecodeError, TypeError):
    ALLOWED_ORIGINS = [o.strip() for o in _raw_origins.split(",") if o.strip()]
