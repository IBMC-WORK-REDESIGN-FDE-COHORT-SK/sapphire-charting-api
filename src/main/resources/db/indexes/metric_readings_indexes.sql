-- ============================================================================
-- Metric Readings API - Database Indexes
-- ============================================================================
-- These indexes optimize the descending time-based queries used by the
-- Metrics Readings API for efficient pagination and filtering.
--
-- Index Strategy:
-- - Composite index on (user_id, time DESC) for each metric table
-- - Supports efficient filtering by user and descending time order
-- - Enables index-only scans for pagination queries
-- - Critical for performance when fetching recent readings
--
-- Performance Impact:
-- - Without index: Full table scan + sort (slow for large datasets)
-- - With index: Index scan in descending order (fast, O(log n) + page size)
--
-- Usage: Execute this script on your TimescaleDB database
-- ============================================================================

-- Heart Rate Index
CREATE INDEX IF NOT EXISTS idx_heartrate_user_time_desc
ON heartrate (user_id, time DESC);

COMMENT ON INDEX idx_heartrate_user_time_desc IS 
'Optimizes descending time queries for heart rate readings by user';

-- Glucose Index
CREATE INDEX IF NOT EXISTS idx_glucose_user_time_desc
ON glucose (user_id, time DESC);

COMMENT ON INDEX idx_glucose_user_time_desc IS 
'Optimizes descending time queries for glucose readings by user';

-- Blood Pressure Index
CREATE INDEX IF NOT EXISTS idx_bloodpressure_user_time_desc
ON bloodpressure (user_id, time DESC);

COMMENT ON INDEX idx_bloodpressure_user_time_desc IS 
'Optimizes descending time queries for blood pressure readings by user';

-- SpO2 Index
CREATE INDEX IF NOT EXISTS idx_spo2_user_time_desc
ON spo2 (user_id, time DESC);

COMMENT ON INDEX idx_spo2_user_time_desc IS 
'Optimizes descending time queries for SpO2 readings by user';

-- Activity Index
CREATE INDEX IF NOT EXISTS idx_activity_user_time_desc
ON activity (user_id, time DESC);

COMMENT ON INDEX idx_activity_user_time_desc IS 
'Optimizes descending time queries for activity readings by user';

-- Sleep Index
CREATE INDEX IF NOT EXISTS idx_sleep_user_time_desc
ON sleep (user_id, time DESC);

COMMENT ON INDEX idx_sleep_user_time_desc IS 
'Optimizes descending time queries for sleep readings by user';

-- Workout Index
CREATE INDEX IF NOT EXISTS idx_workout_user_time_desc
ON workout (user_id, time DESC);

COMMENT ON INDEX idx_workout_user_time_desc IS 
'Optimizes descending time queries for workout readings by user';

-- ============================================================================
-- Index Verification
-- ============================================================================
-- Use these queries to verify index usage and performance:
--
-- 1. Check if indexes exist:
-- SELECT schemaname, tablename, indexname, indexdef
-- FROM pg_indexes
-- WHERE indexname LIKE 'idx_%_user_time_desc'
-- ORDER BY tablename;
--
-- 2. Verify index is being used (EXPLAIN ANALYZE):
-- EXPLAIN ANALYZE
-- SELECT time, metric_name, metric_value, unit, device_id
-- FROM bloodpressure
-- WHERE user_id = 'test_user'
-- ORDER BY time DESC
-- LIMIT 10;
--
-- Expected output should show:
-- "Index Scan using idx_bloodpressure_user_time_desc on bloodpressure"
--
-- 3. Check index size:
-- SELECT
--     schemaname,
--     tablename,
--     indexname,
--     pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
-- FROM pg_stat_user_indexes
-- WHERE indexrelname LIKE 'idx_%_user_time_desc'
-- ORDER BY pg_relation_size(indexrelid) DESC;
--
-- ============================================================================
-- Maintenance Notes
-- ============================================================================
-- - Indexes are automatically maintained by PostgreSQL/TimescaleDB
-- - Consider REINDEX if query performance degrades over time
-- - Monitor index bloat with pg_stat_user_indexes
-- - These indexes will increase write overhead slightly but dramatically
--   improve read performance for the Metrics Readings API
-- ============================================================================

-- Made with Bob