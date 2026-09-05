# Charting API - TimescaleDB Health Metrics Visualization

A Spring Boot 3.x REST API for querying and visualizing health metrics stored in TimescaleDB. Provides flexible charting capabilities with support for multiple metric types, time-based aggregations, and dynamic query generation.

## 🚀 Features

### Chart Query API
- **Multiple Metric Types**: Support for 7 health metric types (Heart Rate, Blood Pressure, Glucose, SpO2, Sleep, Activity, Workout)
- **Flexible Time Aggregation**: RAW, MINUTE, FIVE_MIN, HOUR, DAY resolutions
- **Multiple Aggregation Functions**: AVG, MIN, MAX, SUM, COUNT, P50, P95, P99
- **Multi-Series Support**: Query multiple data series in a single request
- **JSONB Attribute Filtering**: Filter data by device type, position, or custom attributes

### Metrics Readings API (NEW)
- **Paginated Tabular Data**: Efficient pagination with metadata in response headers
- **Health Status Computation**: Automatic status evaluation (NORMAL, ELEVATED, HIGH, etc.)
- **Time-Based Filtering**: Filter readings by time range
- **Multi-Component Metrics**: Support for metrics with multiple values (e.g., systolic/diastolic)
- **Optimized Queries**: Descending index scans for fast recent-data access

### Common Features
- **SQL Injection Protection**: Parameterized queries with input validation
- **Comprehensive Error Handling**: Standardized error responses
- **JWT Authentication**: Secure access with Keycloak integration
- **OpenAPI Documentation**: Interactive Swagger UI
- **Production Ready**: Logging, validation, and connection pooling

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ with TimescaleDB extension
- Database with health metrics tables (see Database Setup)

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.2.2
- **Language**: Java 17
- **Database**: PostgreSQL + TimescaleDB
- **Build Tool**: Maven
- **Documentation**: Springdoc OpenAPI 3
- **Utilities**: Lombok, Jackson

## 📦 Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd ChartingAPI
```

### 2. Configure Database

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/healthdb
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Or run with development profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on `http://localhost:8080`

## 📊 Database Setup

### TimescaleDB Tables

The API expects the following tables to exist in your PostgreSQL database:

```sql
-- Example: Heart Rate Table
CREATE TABLE IF NOT EXISTS public.heartrate (
    request_id text,
    device_id text,
    user_id text NOT NULL,
    scope_name text,
    scope_version text,
    metric_name text NOT NULL,
    unit text,
    attributes jsonb,
    start_time bigint,
    "time" bigint NOT NULL,
    metric_value double precision,
    ingestion_timestamp timestamp without time zone,
    ingestion_time timestamp without time zone,
    schema_version text,
    CONSTRAINT heartrate_pkey PRIMARY KEY ("time", user_id, metric_name)
);

-- Convert to TimescaleDB hypertable
SELECT create_hypertable('heartrate', 'time', chunk_time_interval => 86400000000);
```

**Required Tables**:
- `activity`
- `bloodpressure`
- `devices`
- `glucose`
- `heartrate`
- `sleep`
- `spo2`
- `workout`

### Sample Data

```sql
-- Insert sample heart rate data
INSERT INTO heartrate (user_id, metric_name, time, metric_value, device_id)
VALUES 
  ('u1', 'heartrate', 1707264000000, 72.0, 'device1'),
  ('u1', 'heartrate', 1707264300000, 75.0, 'device1'),
  ('u1', 'heartrate', 1707264600000, 78.0, 'device1');
```

## 🔌 API Documentation

### Swagger UI

Access interactive API documentation at:
```
http://localhost:8080/swagger-ui.html
```

### OpenAPI Specification

JSON specification available at:
```
http://localhost:8080/v3/api-docs
```

## 📝 API Endpoints

### 1. Chart Query API

#### POST /api/v1/charts/query

Query aggregated chart data from TimescaleDB for visualization.

**Request Body**:

```json
{
  "userId": "string",
  "metricType": "HEARTRATE|GLUCOSE|SPO2|BLOODPRESSURE|SLEEP|ACTIVITY|WORKOUT",
  "timeRange": {
    "from": 1707264000000,
    "to": 1707350399000
  },
  "resolution": "RAW|MINUTE|FIVE_MIN|HOUR|DAY",
  "aggregation": "AVG|MIN|MAX|SUM|COUNT|P50|P95|P99",
  "series": [
    {
      "name": "string",
      "metricName": "string",
      "attributeFilter": {
        "key": "value"
      },
      "aggregation": "AVG|MIN|MAX|SUM|COUNT|P50|P95|P99"
    }
  ]
}
```

**Response**:

```json
{
  "chartType": "LINE|BAR",
  "xAxis": "time",
  "series": [
    {
      "name": "string",
      "points": [
        {
          "timestamp": 1707264000000,
          "value": 72.5
        }
      ]
    }
  ]
}
```

### 2. Metrics Readings API

#### GET /api/v1/metrics/{metric}/readings

Retrieve paginated metric readings in tabular format with computed health status.

**Path Parameters**:
- `metric`: Metric type (e.g., `blood_pressure`, `glucose`, `heartrate`, `spo2`)

**Query Parameters**:
- `userId` (required): User identifier
- `page` (optional): Page number, 0-based (default: 0)
- `size` (optional): Page size, max 50 (default: 10)
- `from` (optional): Start time filter (epoch milliseconds)
- `to` (optional): End time filter (epoch milliseconds)

**Response Headers** (Pagination Metadata):
- `X-Page-Number`: Current page number
- `X-Page-Size`: Items per page
- `X-Total-Elements`: Total number of readings
- `X-Total-Pages`: Total number of pages
- `X-Has-Next`: Whether next page exists
- `X-Has-Previous`: Whether previous page exists

**Response Body**:

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

**Health Status Categories**:

| Metric | Status Values | Criteria |
|--------|---------------|----------|
| Blood Pressure | NORMAL, ELEVATED, STAGE_1, STAGE_2, CRISIS | Based on AHA guidelines |
| Glucose | LOW, NORMAL, ELEVATED, HIGH | Based on fasting glucose levels |
| Heart Rate | BRADYCARDIA, NORMAL, TACHYCARDIA | Based on resting heart rate |
| SpO2 | CRITICAL, LOW, NORMAL | Based on oxygen saturation |

## 📚 Usage Examples

### Chart Query API Examples

#### Example 1: Heart Rate Trends (5-Minute Intervals)

**Request**:

```bash
curl -X POST http://localhost:8080/api/v1/charts/query \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "u1",
    "metricType": "HEARTRATE",
    "timeRange": {
      "from": 1707264000000,
      "to": 1707350399000
    },
    "resolution": "FIVE_MIN",
    "aggregation": "AVG",
    "series": [
      {
        "name": "Heart Rate",
        "metricName": "heartrate",
        "aggregation": "AVG"
      }
    ]
  }'
```

**Generated SQL**:

```sql
SELECT 
  time_bucket('5 minutes', to_timestamp(time/1000)) AS bucket,
  AVG(metric_value) AS value
FROM heartrate
WHERE user_id = 'u1'
  AND time >= 1707264000000
  AND time <= 1707350399000
  AND metric_name = 'heartrate'
GROUP BY bucket
ORDER BY bucket;
```

### Example 2: Blood Pressure (Systolic & Diastolic)

**Request**:

```bash
curl -X POST http://localhost:8080/api/v1/charts/query \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "u1",
    "metricType": "BLOODPRESSURE",
    "timeRange": {
      "from": 1706659200000,
      "to": 1707264000000
    },
    "resolution": "DAY",
    "series": [
      {
        "name": "Systolic",
        "metricName": "systolic",
        "aggregation": "AVG"
      },
      {
        "name": "Diastolic",
        "metricName": "diastolic",
        "aggregation": "AVG"
      }
    ]
  }'
```

### Example 3: Activity Summary with Attribute Filter

**Request**:

```bash
curl -X POST http://localhost:8080/api/v1/charts/query \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "u1",
    "metricType": "ACTIVITY",
    "timeRange": {
      "from": 1706659200000,
      "to": 1707264000000
    },
    "resolution": "DAY",
    "aggregation": "SUM",
    "series": [
      {
        "name": "Steps (Apple Watch)",
        "metricName": "steps",
        "attributeFilter": {
          "device_type": "Apple Watch"
        },
        "aggregation": "SUM"
      }
    ]
  }'
```

#### Example 4: Glucose Percentiles

**Request**:

```bash
curl -X POST http://localhost:8080/api/v1/charts/query \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "u1",
    "metricType": "GLUCOSE",
    "timeRange": {
      "from": 1706659200000,
      "to": 1707264000000
    },
    "resolution": "HOUR",
    "series": [
      {
        "name": "Median Glucose",
        "metricName": "glucose",
        "aggregation": "P50"
      },
      {
        "name": "95th Percentile",
        "metricName": "glucose",
        "aggregation": "P95"
      }
    ]
  }'
```

### Metrics Readings API Examples

#### Example 5: Get Recent Blood Pressure Readings

**Request**:

```bash
curl -X GET "http://localhost:8080/api/v1/metrics/blood_pressure/readings?userId=u1&page=0&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response Headers**:
```
X-Page-Number: 0
X-Page-Size: 10
X-Total-Elements: 154
X-Total-Pages: 16
X-Has-Next: true
X-Has-Previous: false
```

**Response Body**:
```json
{
  "metric": "blood_pressure",
  "userId": "u1",
  "data": [
    {
      "timestamp": "2026-02-10T08:30:00Z",
      "values": {
        "systolic": 145,
        "diastolic": 92
      },
      "unit": "mmHg",
      "source": "device_123",
      "status": "STAGE_2"
    },
    {
      "timestamp": "2026-02-10T07:15:00Z",
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

#### Example 6: Get Glucose Readings with Time Filter

**Request**:

```bash
curl -X GET "http://localhost:8080/api/v1/metrics/glucose/readings?userId=u1&from=1707292800000&to=1707379200000&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response**:
```json
{
  "metric": "glucose",
  "userId": "u1",
  "data": [
    {
      "timestamp": "2026-02-07T14:30:00Z",
      "values": {
        "glucose": 95
      },
      "unit": "mg/dL",
      "source": "device_456",
      "status": "NORMAL"
    },
    {
      "timestamp": "2026-02-07T11:00:00Z",
      "values": {
        "glucose": 145
      },
      "unit": "mg/dL",
      "source": "device_456",
      "status": "HIGH"
    }
  ]
}
```

## 🔧 Configuration

### Application Properties

| Property | Description | Default |
|----------|-------------|---------|
| `spring.datasource.url` | Database connection URL | `jdbc:postgresql://localhost:5432/healthdb` |
| `spring.datasource.username` | Database username | `postgres` |
| `spring.datasource.password` | Database password | `password` |
| `spring.datasource.hikari.maximum-pool-size` | Max connection pool size | `10` |
| `server.port` | Application port | `8080` |
| `logging.level.com.health.charting` | Application log level | `DEBUG` |

### Environment Variables

You can override properties using environment variables:

```bash
export DB_HOST=prod-db.example.com
export DB_PORT=5432
export DB_NAME=healthdb
export DB_USERNAME=app_user
export DB_PASSWORD=secure_password
```

## 🏗️ Project Structure

```
ChartingAPI/
├── src/main/java/com/health/charting/
│   ├── ChartingApiApplication.java          # Main application class
│   ├── config/
│   │   ├── DatabaseConfig.java              # Database configuration
│   │   └── OpenApiConfig.java               # Swagger configuration
│   ├── controller/
│   │   └── ChartController.java             # REST endpoints
│   ├── dto/
│   │   ├── request/
│   │   │   ├── ChartQueryRequest.java       # Request DTO
│   │   │   ├── SeriesSpec.java              # Series specification
│   │   │   └── TimeRange.java               # Time range
│   │   └── response/
│   │       ├── ChartResponse.java           # Response DTO
│   │       ├── Series.java                  # Data series
│   │       ├── DataPoint.java               # Single data point
│   │       └── ErrorResponse.java           # Error response
│   ├── enums/
│   │   ├── MetricType.java                  # Metric types
│   │   ├── Aggregation.java                 # Aggregation functions
│   │   ├── Resolution.java                  # Time resolutions
│   │   └── ChartType.java                   # Chart types
│   ├── service/
│   │   ├── ChartQueryService.java           # Service interface
│   │   └── impl/
│   │       └── ChartQueryServiceImpl.java   # Service implementation
│   ├── util/
│   │   └── SqlQueryBuilder.java             # SQL query builder
│   └── exception/
│       ├── GlobalExceptionHandler.java      # Global exception handler
│       ├── InvalidQueryException.java       # Custom exceptions
│       └── DataNotFoundException.java
└── src/main/resources/
    ├── application.properties               # Main configuration
    └── application-dev.properties           # Dev configuration
```

## 🔒 Security

### SQL Injection Prevention

- All queries use parameterized statements via `NamedParameterJdbcTemplate`
- Input validation on all user-provided values
- Whitelist validation for table names and column names
- SQL keyword detection in metric names and attribute filters

### Input Validation

- Bean Validation annotations on all DTOs
- Custom validators for complex rules
- Time range validation (from < to)
- Series count limits (1-10 series per request)

## 🧪 Testing

### Run Tests

```bash
mvn test
```

### Test Coverage

```bash
mvn clean test jacoco:report
```

Report available at: `target/site/jacoco/index.html`

## 📈 Performance

### Optimization Tips

1. **Indexes**: Ensure proper indexes on `(user_id, time, metric_name)`
   - For Metrics Readings API, create descending indexes: `CREATE INDEX idx_{table}_user_time_desc ON {table} (user_id, time DESC);`
   - See `src/main/resources/db/indexes/metric_readings_indexes.sql` for complete index setup
2. **Connection Pooling**: Adjust HikariCP settings based on load
3. **Time Bucketing**: Use appropriate resolution to reduce data points
4. **Chunk Intervals**: Configure TimescaleDB chunk intervals appropriately
5. **Pagination**: Use appropriate page sizes (default: 10, max: 50) for Metrics Readings API

### Monitoring

- Application logs: `logs/application.log`
- SQL query logs: Enable with `logging.level.org.springframework.jdbc=TRACE`
- Connection pool metrics: Available via Spring Boot Actuator (if enabled)

## 🐛 Troubleshooting

### Common Issues

**Issue**: Connection refused to database
```
Solution: Verify PostgreSQL is running and connection details are correct
```

**Issue**: TimescaleDB extension not found
```sql
Solution: Install TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;
```

**Issue**: No data returned
```
Solution: Check time range, user_id, and metric_name are correct
Enable DEBUG logging to see generated SQL queries
```

## 📄 License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## 👥 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

For issues and questions:
- Create an issue in the repository
- Email: support@healthcharting.com

## 🗺️ Roadmap

- [x] Metrics Readings API with pagination and status computation
- [ ] Add caching layer (Redis)
- [ ] Implement rate limiting
- [ ] Add WebSocket support for real-time data
- [ ] Export to CSV/Excel
- [ ] Advanced anomaly detection
- [ ] Multi-tenant support
- [ ] GraphQL API

## 📚 Additional Resources

- [TimescaleDB Documentation](https://docs.timescale.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [OpenAPI Specification](https://swagger.io/specification/)

---

**Version**: 1.0.0  
**Last Updated**: 2026-02-07  
**Maintained by**: Health Charting API Team