"""Async Snowflake database with dependency injection."""
import asyncio
import snowflake.connector
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Callable, TypeVar
from app.core.config import SNOWFLAKE_CONFIG

T = TypeVar("T")

_executor = ThreadPoolExecutor(max_workers=10)


class SnowflakeDB:
    """Async Snowflake database class for dependency injection."""
    
    async def query(self, sql: str, params: tuple = None):
        """Async query."""
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(_executor, self._sync_query, sql, params)
    
    def _sync_query(self, sql: str, params: tuple = None):
        """Sync query implementation."""
        conn = snowflake.connector.connect(**SNOWFLAKE_CONFIG)
        cur = conn.cursor()
        try:
            if params:
                cur.execute(sql, params)
            else:
                cur.execute(sql)
            
            columns = [desc[0] for desc in cur.description] if cur.description else []
            rows = cur.fetchall()
            return [dict(zip(columns, row)) for row in rows]
        finally:
            cur.close()
            conn.close()
    
    async def execute(self, sql: str, params: tuple = None):
        """Async execute (insert/update)."""
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(_executor, self._sync_execute, sql, params)
    
    def _sync_execute(self, sql: str, params: tuple = None):
        """Sync execute implementation."""
        conn = snowflake.connector.connect(**SNOWFLAKE_CONFIG)
        cur = conn.cursor()
        try:
            if params:
                cur.execute(sql, params)
            else:
                cur.execute(sql)
            conn.commit()
            return cur.rowcount
        finally:
            cur.close()
            conn.close()
    
    async def fetch_one(self, sql: str, params: tuple = None):
        """Fetch single row."""
        rows = await self.query(sql, params)
        return rows[0] if rows else None

    async def run_transaction(self, fn: Callable[[Any], T]) -> T:
        """Run ``fn(conn)`` on one connection; commit on success, rollback on error."""
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(_executor, self._sync_transaction, fn)

    def _sync_transaction(self, fn: Callable[[Any], T]) -> T:
        cfg = {**SNOWFLAKE_CONFIG, "autocommit": False}
        conn = snowflake.connector.connect(**cfg)
        try:
            result = fn(conn)
            conn.commit()
            return result
        except Exception:
            conn.rollback()
            raise
        finally:
            conn.close()


# Dependency to inject
def get_db() -> SnowflakeDB:
    """Dependency: Get database instance."""
    return SnowflakeDB()
