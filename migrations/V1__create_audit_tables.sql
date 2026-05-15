-- Flyway migration for audit tables
CREATE TABLE IF NOT EXISTS request_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     UUID NOT NULL UNIQUE,
    timestamp    TIMESTAMPTZ NOT NULL,
    ip           INET NOT NULL,
    api_key      TEXT,
    endpoint     TEXT NOT NULL,
    method       VARCHAR(10),
    status_code  SMALLINT,
    payload_size INTEGER,
    user_agent   TEXT,
    latency_ms   INTEGER,
    content_type TEXT,
    forwarded_for TEXT,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_request_events_ip_timestamp ON request_events (ip, timestamp);
CREATE INDEX IF NOT EXISTS idx_request_events_api_key_timestamp ON request_events (api_key, timestamp);
CREATE INDEX IF NOT EXISTS idx_request_events_endpoint_timestamp ON request_events (endpoint, timestamp);

CREATE TABLE IF NOT EXISTS decision_events (
    id           BIGSERIAL PRIMARY KEY,
    event_id     UUID NOT NULL,
    request_event_id UUID,
    timestamp    TIMESTAMPTZ NOT NULL,
    ip          INET,
    api_key     TEXT,
    action      VARCHAR(20) NOT NULL,
    score       NUMERIC(5,4),
    reason      TEXT,
    expires_at  TIMESTAMPTZ,
    triggered_rules TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_decision_events_action_timestamp ON decision_events (action, timestamp);
CREATE INDEX IF NOT EXISTS idx_decision_events_ip_timestamp ON decision_events (ip, timestamp);
CREATE INDEX IF NOT EXISTS idx_decision_events_api_key_timestamp ON decision_events (api_key, timestamp);