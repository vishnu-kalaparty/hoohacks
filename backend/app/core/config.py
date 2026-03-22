"""Configuration settings."""
import json
import os
from dotenv import load_dotenv

load_dotenv()

# =============================================================================
# SNOWFLAKE CONFIGURATION
# =============================================================================
SNOWFLAKE_CONFIG = {
    "user": os.getenv("SNOWFLAKE_USER"),
    "password": os.getenv("SNOWFLAKE_PASSWORD"),
    "account": os.getenv("SNOWFLAKE_ACCOUNT"),
    "database": os.getenv("SNOWFLAKE_DATABASE", "CADENCE"),
    "schema": os.getenv("SNOWFLAKE_SCHEMA", "PUBLIC"),
}

# =============================================================================
# AUTH0 CONFIGURATION
# =============================================================================
AUTH0_CONFIG = {
    "domain": os.getenv("AUTH0_DOMAIN", ""),
    "api_audience": os.getenv("AUTH0_API_AUDIENCE", ""),
    "client_id": os.getenv("AUTH0_CLIENT_ID", ""),
    "client_secret": os.getenv("AUTH0_CLIENT_SECRET", ""),
    "algorithms": os.getenv("AUTH0_ALGORITHMS", "RS256").split(","),
}

# =============================================================================
# APP CONFIGURATION
# =============================================================================
PROJECT_NAME = os.getenv("PROJECT_NAME", "Auris")
VERSION = os.getenv("VERSION", "0.1.0")
DEBUG = os.getenv("DEBUG", "false").lower() == "true"
API_V1_STR = os.getenv("API_V1_STR", "/api/v1")

# CORS
ALLOWED_HOSTS = os.getenv(
    "ALLOWED_HOSTS",
    '["http://localhost:3000","http://localhost:8501","http://localhost:8000"]'
)
try:
    import json
    ALLOWED_HOSTS = json.loads(ALLOWED_HOSTS)
except json.JSONDecodeError:
    ALLOWED_HOSTS = ["*"]
_raw_origins = os.getenv("ALLOWED_ORIGINS", "")
try:
    ALLOWED_ORIGINS: list[str] = json.loads(_raw_origins) if _raw_origins else []
except (json.JSONDecodeError, TypeError):
    ALLOWED_ORIGINS = [o.strip() for o in _raw_origins.split(",") if o.strip()]
