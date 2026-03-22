-- HIPAA Audit Log
-- Tracks every API access to PHI-related routes.

CREATE TABLE IF NOT EXISTS CADENCE.PUBLIC.AUDIT_LOG (
    AUDIT_ID        NUMBER AUTOINCREMENT PRIMARY KEY,
    LOGGED_AT       TIMESTAMP_NTZ DEFAULT CURRENT_TIMESTAMP(),
    USER_IDENTITY   VARCHAR(256)  NOT NULL,       -- Auth0 sub / 'anonymous' pre-auth
    HTTP_METHOD     VARCHAR(10)   NOT NULL,
    PATH            VARCHAR(512)  NOT NULL,
    QUERY_STRING    VARCHAR(2048),
    SOURCE_IP       VARCHAR(45),                   -- supports IPv6
    STATUS_CODE     NUMBER(3),
    RESPONSE_TIME_MS NUMBER(10,2)
);

-- Fast lookups by user and time range (typical HIPAA audit queries).
CREATE INDEX IF NOT EXISTS IDX_AUDIT_USER_TIME
    ON CADENCE.PUBLIC.AUDIT_LOG (USER_IDENTITY, LOGGED_AT);

-- Fast lookups by path (e.g. "who accessed /patients/42?").
CREATE INDEX IF NOT EXISTS IDX_AUDIT_PATH_TIME
    ON CADENCE.PUBLIC.AUDIT_LOG (PATH, LOGGED_AT);
