-- ============================================================================
-- Temperature Retention Policy and Continuous Aggregate Migration
-- ============================================================================
-- Applies a 90-day raw retention policy on the temperature hypertable and
-- creates a temperature_daily_agg continuous aggregate for efficient trend queries.
-- Requires TimescaleDB extension (safe no-op if absent).
-- ============================================================================

-- Apply 90-day raw data retention policy on the temperature hypertable
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        -- add_retention_policy is idempotent when if_not_exists => TRUE
        PERFORM add_retention_policy(
            'temperature',
            INTERVAL '90 days',
            if_not_exists => TRUE
        );
    END IF;
END $$;

-- Create daily continuous aggregate over the temperature hypertable
-- Computes per-user, per-day min/max/avg of normalized_value_celsius
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        -- Create continuous aggregate (idempotent via IF NOT EXISTS)
        EXECUTE $q$
            CREATE MATERIALIZED VIEW IF NOT EXISTS temperature_daily_agg
            WITH (timescaledb.continuous) AS
            SELECT
                time_bucket('1 day', time) AS bucket,
                user_id,
                COUNT(*)                        AS reading_count,
                MIN(normalized_value_celsius)   AS min_celsius,
                MAX(normalized_value_celsius)   AS max_celsius,
                AVG(normalized_value_celsius)   AS avg_celsius
            FROM temperature
            GROUP BY bucket, user_id
            WITH NO DATA
        $q$;

        -- Enable automatic refresh: keep the last 91 days up to date
        PERFORM add_continuous_aggregate_policy(
            'temperature_daily_agg',
            start_offset => INTERVAL '91 days',
            end_offset   => INTERVAL '1 hour',
            schedule_interval => INTERVAL '1 day',
            if_not_exists => TRUE
        );
    END IF;
END $$;

COMMENT ON MATERIALIZED VIEW temperature_daily_agg IS
'Daily aggregates (min/max/avg °C) per user over the temperature hypertable. ' ||
'Refreshed automatically by TimescaleDB continuous aggregate policy.';
