# Metrics Readings API - Implementation Plan

## 📋 Overview

Design and implement a new REST API endpoint that provides health metrics data in a tabular, paginated format. This API will sit on top of the existing TimescaleDB infrastructure and provide efficient access to raw metric readings with computed status indicators.

---

## 🎯 Requirements Summary

### Endpoint Specification
```
GET /api/v1/metrics/{metricName}/readings
```

### Query Parameters
| Parameter | Required | Type | Default | Description |
|-----------|----------|------|---------|-------------|
| userId | ✅ Yes | String | - | Metric owner identifier |
| page | ❌ No | Integer | 0 | Page index (0-based) |
| size | ❌ No | Integer | 10 | Page size (max: 100) |
| from | ❌ No | Long | - | Start time (epoch milliseconds) |
| to | ❌ No | Long | - | End time (epoch milliseconds) |
| sort | ❌ No | String | time,desc | Sort specification |

### Response Structure
```json
{
  "metric": "blood_pressure",
  "userId": "u1",
  "data": [
    {
      "timestamp": "2026-02-07T10:30:00Z",
      "values": {
        "systolic": 120,
        "diastolic": 80
      },
      "unit": "mmHg",
      "source": "device_123",
      "status": "NORMAL"
    }
  ]
}
```

**Pagination Headers** (instead of response body):
- `X-Page-Number`: Current page number
- `X-Page-Size`: Page size
- `X-Total-Elements`: Total number of elements
- `X-Total-Pages`: Total number of pages
- `X-Has-Next`: Boolean indicating if next page exists
- `X-Has-Previous`: Boolean indicating if previous page exists

---

## 🏗️ Architecture Design

### System Flow Diagram

```mermaid
graph TB
    Client[Client Application]
    Controller[MetricReadingController]
    Service[MetricReadingService]
    Repository[MetricReadingRepository]
    Aggregator[TimestampAggregator]
    Evaluator[StatusEvaluator]
    Registry[MetricStatusEvaluatorRegistry]
    DB[(TimescaleDB)]
    
    Client -->|GET /api/v1/metrics/{metric}/readings| Controller
    Controller -->|Validate & Extract Params| Controller
    Controller --> Service
    Service -->|Resolve MetricType| Service
    Service --> Repository
    Repository -->|Execute Query with Index Scan| DB
    DB -->|Raw Rows| Repository
    Repository --> Service
    Service --> Aggregator
    Aggregator -->|Group by Timestamp| Aggregator
    Aggregator --> Service
    Service --> Registry
    Registry -->|Get Evaluator for Metric| Evaluator
    Evaluator -->|Compute Status| Service
    Service -->|MetricReadingDto List| Controller
    Controller -->|Add Pagination Headers| Controller
    Controller -->|JSON Response| Client
    
    style Client fill:#e1f5ff
    style Controller fill:#fff4e1
    style Service fill:#e8f5e9
    style DB fill:#f3e5f5
    style Evaluator fill:#ffe0b2
```

### Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌────────────────────────────────────────────────────┐    │
│  │         MetricReadingController                     │    │
│  │  - GET /api/v1/metrics/{metric}/readings           │    │
│  │  - Request validation                               │    │
│  │  - Pagination header management                     │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     Service Layer                            │
│  ┌────────────────────────────────────────────────────┐    │
│  │         MetricReadingService                        │    │
│  │  - Business logic orchestration                     │    │
│  │  - MetricType resolution                            │    │
│  │  - Timestamp aggregation                            │    │
│  │  - Status computation                               │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │    MetricStatusEvaluatorRegistry                    │    │
│  │  - Manages status evaluators by metric type         │    │
│  │  - Pluggable evaluator pattern                      │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                           │
│  ┌────────────────────────────────────────────────────┐    │
│  │       MetricReadingRepository                       │    │
│  │  - Database query execution                         │    │
│  │  - Efficient descending index scan                  │    │
│  │  - Pagination support                               │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                                │
│                  TimescaleDB Tables                          │
│  - heartrate, glucose, bloodpressure, spo2, etc.            │
│  - Indexed by (user_id, metric_name, time DESC)            │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Component Specifications

### 1. DTOs (Data Transfer Objects)

#### MetricReadingDto
```java
package com.health.charting.dto.response;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricReadingDto {
    private Instant timestamp;
    private Map<String, Number> values;
    private String unit;
    private String source;
    private String status;
}
```

#### MetricReadingsResponse
```java
package com.health.charting.dto.response;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricReadingsResponse {
    private String metric;
    private String userId;
    private List<MetricReadingDto> data;
}
```

### 2. Status Evaluation System

#### MetricStatusEvaluator Interface
```java
package com.health.charting.service.evaluator;

public interface MetricStatusEvaluator {
    /**
     * Evaluate the status of a metric reading based on its values.
     * 
     * @param values Map of metric component names to their numeric values
     * @return Optional status string, empty if status cannot be determined
     */
    Optional<String> evaluate(Map<String, Number> values);
    
    /**
     * Get the metric type this evaluator handles.
     */
    MetricType getMetricType();
}
```

#### Implementations for Each Metric Type

**BloodPressureStatusEvaluator**
- CRISIS: systolic ≥ 180 OR diastolic ≥ 120
- STAGE_2: systolic ≥ 140 OR diastolic ≥ 90
- STAGE_1: systolic ≥ 130 OR diastolic ≥ 80
- ELEVATED: systolic ≥ 120
- NORMAL: Otherwise

**GlucoseStatusEvaluator**
- HIGH: glucose > 140 (mg/dL)
- ELEVATED: glucose > 100
- NORMAL: glucose ≤ 100
- LOW: glucose < 70

**HeartRateStatusEvaluator**
- TACHYCARDIA: heartrate > 100
- NORMAL: 60 ≤ heartrate ≤ 100
- BRADYCARDIA: heartrate < 60

**SpO2StatusEvaluator**
- NORMAL: spo2 ≥ 95
- LOW: 90 ≤ spo2 < 95
- CRITICAL: spo2 < 90

**DefaultStatusEvaluator** (for metrics without specific rules)
- Returns Optional.empty()

### 3. Repository Layer

#### MetricReadingRepository
```java
package com.health.charting.repository;

@Repository
@RequiredArgsConstructor
public class MetricReadingRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    
    /**
     * Fetch metric readings with efficient descending index scan.
     * Groups rows by timestamp to create composite readings.
     */
    public List<MetricReadingDto> fetchReadings(
        MetricType metricType,
        String userId,
        int page,
        int size,
        Long fromTime,
        Long toTime
    );
    
    /**
     * Count total readings for pagination.
     */
    public long countReadings(
        MetricType metricType,
        String userId,
        Long fromTime,
        Long toTime
    );
}
```

**Key SQL Query Pattern:**
```sql
-- Efficient descending index scan with timestamp grouping
SELECT 
    time,
    metric_name,
    metric_value,
    unit,
    device_id
FROM {table_name}
WHERE user_id = :userId
  AND (:fromTime IS NULL OR time >= :fromTime)
  AND (:toTime IS NULL OR time <= :toTime)
ORDER BY time DESC
LIMIT :size OFFSET :offset
```

**Required Index:**
```sql
CREATE INDEX IF NOT EXISTS idx_{table}_user_time_desc
ON {table_name} (user_id, time DESC);
```

### 4. Service Layer

#### MetricReadingService
```java
package com.health.charting.service;

@Service
@RequiredArgsConstructor
public class MetricReadingService {
    private final MetricReadingRepository repository;
    private final MetricStatusEvaluatorRegistry evaluatorRegistry;
    
    /**
     * Fetch paginated metric readings with status computation.
     */
    public MetricReadingsResponse fetchReadings(
        String metricName,
        String userId,
        int page,
        int size,
        Long fromTime,
        Long toTime
    );
    
    /**
     * Calculate pagination metadata.
     */
    public PageInfo calculatePageInfo(
        long totalElements,
        int page,
        int size
    );
}
```

**Processing Flow:**
1. Resolve MetricType from metricName
2. Fetch raw rows from repository
3. Group rows by timestamp (for multi-component metrics)
4. Get appropriate status evaluator
5. Compute status for each reading
6. Build response with pagination info

### 5. Controller Layer

#### MetricReadingController
```java
package com.health.charting.controller;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricReadingController {
    private final MetricReadingService service;
    
    @GetMapping("/{metric}/readings")
    public ResponseEntity<MetricReadingsResponse> getReadings(
        @PathVariable String metric,
        @RequestParam String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) Long from,
        @RequestParam(required = false) Long to
    );
}
```

**Response Headers:**
- X-Page-Number
- X-Page-Size
- X-Total-Elements
- X-Total-Pages
- X-Has-Next
- X-Has-Previous

---

## 🗄️ Database Optimization

### Required Indexes

For each metric table (heartrate, glucose, bloodpressure, spo2, activity, sleep, workout):

```sql
-- Optimized for descending time queries with user filtering
CREATE INDEX IF NOT EXISTS idx_heartrate_user_time_desc
ON heartrate (user_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_glucose_user_time_desc
ON glucose (user_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_bloodpressure_user_time_desc
ON bloodpressure (user_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_spo2_user_time_desc
ON spo2 (user_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_activity_user_time_desc
ON activity (user_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_sleep_user_time_desc
ON sleep (user_id, time DESC);

CREATE INDEX IF NOT EXISTS idx_workout_user_time_desc
ON workout (user_id, time DESC);
```

### Query Performance Strategy

1. **Index Scan Direction**: Use DESC index for efficient recent-data queries
2. **Limit + Offset**: Pagination at database level
3. **Timestamp Grouping**: In-memory aggregation after fetch (minimal overhead)
4. **Count Optimization**: Separate count query with same filters

---

## 📝 Implementation Checklist

### Phase 1: Core Infrastructure
- [ ] Create DTOs package structure
  - [ ] MetricReadingDto
  - [ ] MetricReadingsResponse
  - [ ] PageInfo (for internal use)
- [ ] Create status evaluator framework
  - [ ] MetricStatusEvaluator interface
  - [ ] BloodPressureStatusEvaluator
  - [ ] GlucoseStatusEvaluator
  - [ ] HeartRateStatusEvaluator
  - [ ] SpO2StatusEvaluator
  - [ ] DefaultStatusEvaluator
  - [ ] MetricStatusEvaluatorRegistry

### Phase 2: Data Access Layer
- [ ] Create MetricReadingRepository
  - [ ] fetchReadings() method with timestamp grouping
  - [ ] countReadings() method
  - [ ] SQL query builder for dynamic table selection
- [ ] Create database index scripts
  - [ ] Index creation SQL for all metric tables
  - [ ] Migration documentation

### Phase 3: Business Logic Layer
- [ ] Create MetricReadingService
  - [ ] fetchReadings() orchestration
  - [ ] Timestamp aggregation logic
  - [ ] Status computation integration
  - [ ] Pagination calculation
- [ ] Create TimestampAggregator utility
  - [ ] Group rows by timestamp
  - [ ] Build values map

### Phase 4: API Layer
- [ ] Create MetricReadingController
  - [ ] GET endpoint implementation
  - [ ] Request validation
  - [ ] Pagination header management
  - [ ] Error handling
- [ ] Update SecurityConfig
  - [ ] Add /api/v1/metrics/** to authenticated paths
  - [ ] Configure scope requirements

### Phase 5: Documentation & Testing
- [ ] Create API documentation
  - [ ] OpenAPI/Swagger annotations
  - [ ] Usage examples
  - [ ] README section
- [ ] Unit tests
  - [ ] Status evaluator tests
  - [ ] Service layer tests
  - [ ] Repository tests
- [ ] Integration tests
  - [ ] End-to-end API tests
  - [ ] Pagination tests
  - [ ] Error scenario tests

---

## 🔍 Key Design Decisions

### 1. Timestamp Aggregation Strategy
**Decision**: Group rows by timestamp in-memory after database fetch

**Rationale**:
- Database stores multi-component metrics as separate rows (e.g., systolic and diastolic)
- Grouping in SQL would require complex window functions or self-joins
- In-memory grouping is simpler and performs well for paginated results
- Typical page size (10-100 rows) makes in-memory processing negligible

### 2. Status Computation
**Decision**: Pluggable evaluator pattern with registry

**Rationale**:
- Different metrics have different status rules
- Easy to add new metrics without modifying core logic
- Testable in isolation
- Optional status (some metrics may not have status rules)

### 3. Pagination Headers
**Decision**: Use custom HTTP headers instead of response body

**Rationale**:
- Cleaner response body focused on data
- Standard pattern for REST APIs
- Easier for clients to parse pagination metadata
- Follows HTTP best practices

### 4. Table Resolution
**Decision**: Use MetricType enum to map to table names

**Rationale**:
- Reuses existing MetricType infrastructure
- Type-safe table name resolution
- Prevents SQL injection
- Consistent with existing codebase

---

## 📊 Example API Usage

### Request Example
```bash
GET /api/v1/metrics/blood_pressure/readings?userId=u1&page=0&size=10&from=1707292800000&to=1707379200000
Authorization: Bearer <jwt_token>
```

### Response Example
```json
{
  "metric": "blood_pressure",
  "userId": "u1",
  "data": [
    {
      "timestamp": "2026-02-07T10:30:00Z",
      "values": {
        "systolic": 120,
        "diastolic": 80
      },
      "unit": "mmHg",
      "source": "device_123",
      "status": "NORMAL"
    },
    {
      "timestamp": "2026-02-07T09:15:00Z",
      "values": {
        "systolic": 145,
        "diastolic": 92
      },
      "unit": "mmHg",
      "source": "device_123",
      "status": "STAGE_2"
    }
  ]
}
```

### Response Headers
```
X-Page-Number: 0
X-Page-Size: 10
X-Total-Elements: 154
X-Total-Pages: 16
X-Has-Next: true
X-Has-Previous: false
```

---

## 🚀 Performance Considerations

### Database Query Optimization
- **Index Usage**: Descending index scan for efficient recent-data access
- **Limit/Offset**: Database-level pagination reduces data transfer
- **Selective Columns**: Only fetch required columns
- **Connection Pooling**: Reuse existing HikariCP configuration

### Memory Optimization
- **Streaming**: Process results as they arrive
- **Page Size Limit**: Max 100 items per page prevents memory issues
- **Timestamp Grouping**: O(n) complexity with small n (page size)

### Caching Strategy (Future Enhancement)
- Cache frequently accessed pages
- Cache status computation results
- Use Redis for distributed caching

---

## 🔒 Security Considerations

### Authentication & Authorization
- JWT token validation (existing infrastructure)
- Scope validation for metrics access
- User ID validation (ensure user can only access their own data)

### Input Validation
- Metric name validation against MetricType enum
- Page/size bounds checking
- Timestamp range validation
- SQL injection prevention (parameterized queries)

---

## 📈 Monitoring & Metrics

### Custom Metrics to Add
- `metrics.api.readings.requests.total` - Total requests counter
- `metrics.api.readings.requests.by.metric` - Requests by metric type
- `metrics.api.readings.query.duration` - Query execution time
- `metrics.api.readings.aggregation.duration` - Aggregation time
- `metrics.api.readings.status.computation.duration` - Status computation time
- `metrics.api.readings.errors` - Error counter by type

---

## 🎯 Success Criteria

1. ✅ API returns paginated metric readings in tabular format
2. ✅ Efficient database queries using descending index scan
3. ✅ Multi-component metrics properly aggregated by timestamp
4. ✅ Status computation working for all metric types
5. ✅ Pagination metadata in response headers
6. ✅ Comprehensive error handling
7. ✅ Full test coverage (unit + integration)
8. ✅ API documentation complete
9. ✅ Performance meets requirements (<500ms for typical queries)

---

## 📚 Related Documentation

- [README_METRICS.md](README_METRICS.md) - Existing metrics infrastructure
- [README_JWT_AUTHENTICATION.md](README_JWT_AUTHENTICATION.md) - Authentication setup
- [ProjectPlan.md](ProjectPlan.md) - Overall project architecture

---

**Created**: 2026-02-10  
**Status**: Ready for Implementation  
**Estimated Effort**: 3-4 days