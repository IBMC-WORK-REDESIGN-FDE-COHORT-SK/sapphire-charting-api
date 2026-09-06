-- ============================================================================
-- Temperature Metric Table and Index Migration
-- ============================================================================

CREATE TABLE IF NOT EXISTS temperature (
    time TIMESTAMPTZ NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    metric_name VARCHAR(64) DEFAULT 'temperature',
    metric_value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(16) NOT NULL,
    normalized_value_celsius DOUBLE PRECISION NOT NULL,
    device_id VARCHAR(64),
    measurement_method VARCHAR(32),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Convert to TimescaleDB Hypertable if TimescaleDB extension is present
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        PERFORM create_hypertable('temperature', 'time', if_not_exists => TRUE);
    END IF;
END $$;

-- Composite Index for time-range user queries
CREATE INDEX IF NOT EXISTS idx_temperature_user_time_desc
ON temperature (user_id, time DESC);

COMMENT ON INDEX idx_temperature_user_time_desc IS 
'Optimizes descending time queries for temperature readings by user';
