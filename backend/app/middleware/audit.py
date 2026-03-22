"""HIPAA audit-logging middleware.

Logs every request that touches PHI-related routes to the
CADENCE.PUBLIC.AUDIT_LOG table in Snowflake.  The insert runs in the
existing thread-pool so it never blocks the response.
"""

import logging
import time
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

from app.core.database import SnowflakeDB

logger = logging.getLogger(__name__)

PHI_PREFIXES = ("/patients", "/checkins", "/dashboard", "/therapists")

_INSERT_AUDIT = """
    INSERT INTO CADENCE.PUBLIC.AUDIT_LOG
        (USER_IDENTITY, HTTP_METHOD, PATH, QUERY_STRING,
         SOURCE_IP, STATUS_CODE, RESPONSE_TIME_MS)
    VALUES (%s, %s, %s, %s, %s, %s, %s)
"""


class AuditLogMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        if not any(request.url.path.startswith(p) for p in PHI_PREFIXES):
            return await call_next(request)

        start = time.perf_counter()
        response = await call_next(request)
        elapsed_ms = round((time.perf_counter() - start) * 1000, 2)

        # When auth is wired up, pull identity from request.state or the JWT sub claim.
        user_identity = getattr(request.state, "user_id", None) or "anonymous"
        source_ip = request.client.host if request.client else "unknown"
        query_string = str(request.query_params) or None

        try:
            db = SnowflakeDB()
            await db.execute(
                _INSERT_AUDIT,
                (
                    user_identity,
                    request.method,
                    request.url.path,
                    query_string,
                    source_ip,
                    response.status_code,
                    elapsed_ms,
                ),
            )
        except Exception:
            logger.exception("Failed to write audit log row")

        return response
