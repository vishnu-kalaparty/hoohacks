"""Configuration settings."""
import os
from dotenv import load_dotenv

load_dotenv()

SNOWFLAKE_CONFIG = {
    "user": os.getenv("SNOWFLAKE_USER"),
    "password": os.getenv("SNOWFLAKE_PASSWORD"),
    "account": os.getenv("SNOWFLAKE_ACCOUNT"),
    "database": os.getenv("SNOWFLAKE_DATABASE", "CADENCE"),
    "schema": os.getenv("SNOWFLAKE_SCHEMA", "PUBLIC")
}
