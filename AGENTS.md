# Sapphire Charting API - Agent Documentation

## Overview
The Sapphire Charting API is a Spring Boot REST service that provides flexible querying and visualization capabilities for health metrics stored in TimescaleDB. It enables dynamic chart generation with support for multiple metric types, time-based aggregations, and multi-series data visualization.

## Purpose & Role in Landscape
This service acts as the **data visualization and analytics layer** for health metrics, providing:
- **Time-series Data Queries**: Flexible querying of health metrics with various time resolutions
- **Multi-metric Support**: Heart rate, blood pressure, glucose, SpO2, sleep, activity, and workout data
- **Aggregation Functions**: Statistical analysis (AVG, MIN, MAX, SUM, COUNT, percentiles)
- **Chart Data Generation**: Formatted data ready for frontend charting libraries
- **JSONB Filtering**: Advanced filtering on device attributes and metadata

The API abstracts the complexity of TimescaleDB time-series queries, providing a simple REST interface for the BFF and frontend to retrieve chart-ready data.

## Integration Points
- **Upstream**: 
  - `sapphire-bff-api` - Primary consumer for dashboard charts
  - Frontend applications - Direct chart data queries
- **Downstream Services**:
  - TimescaleDB/PostgreSQL (port 5432) - Time-series health metrics storage
- **Authentication**: OAuth2 Resource Server with JWT validation

## Boundary Conditions

### Query Constraints
- **Time Range**: Must specify valid from/to timestamps (from < to)
- **Series Limit**: 1-10 data series per request
- **Metric Types**: 7 supported types (HEARTRATE, BLOODPRESSURE, GLUCOSE, SPO2, SLEEP, ACTIVITY, WORKOUT)
- **Resolution Options**: RAW, MINUTE, FIVE_MIN, HOUR, DAY
- **Aggregation Functions**: AVG, MIN, MAX, SUM, COUNT, P50, P95, P99

### Data Access Limits
- **User Isolation**: Queries filtered by userId (no cross-user data access)
- **Table Whitelist**: Only predefined health metric tables accessible
- **Column Whitelist**: Only validated metric names and attributes queryable
- **SQL Injection Protection**: Parameterized queries with input validation

### Performance Boundaries
- **Connection Pool**: HikariCP with configurable max connections (default 10)
- **Query Timeout**: Database query timeout enforced
- **Time Bucketing**: Automatic aggregation based on resolution to reduce data points
- **Index Requirements**: Requires indexes on (user_id, time, metric_name)

### Security Constraints
- **JWT Authentication**: Required Bearer token in Authorization header
- **OAuth2 Resource Server**: Token validation via Spring Security
- **Input Validation**: Bean validation on all request DTOs
- **SQL Keyword Detection**: Prevents SQL injection in metric names and filters

### Known Limitations
- **No Real-time Streaming**: Polling-based, not WebSocket
- **No Data Modification**: Read-only API (no POST/PUT/DELETE for data)
- **No Cross-user Analytics**: Single user per query
- **No Export Formats**: Returns JSON only (no CSV/Excel)
- **No Caching Layer**: Direct database queries (Redis caching planned)

## Tech Stack

### Core Framework
- **Java 17** - Programming language
- **Spring Boot 3.2.2** - Application framework
- **Maven 3.9+** - Build tool

### Spring Boot Starters
- **spring-boot-starter-web** - REST API endpoints
- **spring-boot-starter-jdbc** - JDBC database access with NamedParameterJdbcTemplate
- **spring-boot-starter-validation** - Bean validation
- **spring-boot-starter-security** - Security framework
- **spring-boot-starter-oauth2-resource-server** - OAuth2/JWT validation

### Database
- **PostgreSQL 12+** - Relational database
- **TimescaleDB Extension** - Time-series database capabilities
  - Hypertables for efficient time-series storage
  - time_bucket() for time-based aggregations
  - Automatic data partitioning by time

### Security & Authentication
- **Spring Security** - Authentication and authorization
- **OAuth2 Resource Server** - JWT token validation
- **nimbus-jose-jwt 9.37.3** - JWT processing

### API Documentation
- **Springdoc OpenAPI 2.3.0** - OpenAPI 3.0 specification
- **Swagger UI** - Interactive API documentation

### Data & Utilities
- **Jackson Databind** - JSON serialization/deserialization
- **Lombok** - Boilerplate code reduction
- **HikariCP** - Connection pooling (via Spring Boot)

### Observability
- **OpenTelemetry Java Agent** - Distributed tracing (via Docker)
- **Spring Boot Actuator** - Health checks and metrics (implicit)

### Testing
- **spring-boot-starter-test** - Testing framework
- **JUnit Jupiter** - Unit testing

### Deployment
- **Docker** - Multi-stage build (Maven + Eclipse Temurin JRE Alpine)
- **Port 8089** - HTTP REST API
- **Health Check** - `/actuator/health` endpoint
- **Non-root User** - Security-hardened container

## Architecture Pattern
- **REST API** - Synchronous HTTP endpoints
- **Repository Pattern** - JDBC-based data access
- **DTO Pattern** - Request/Response data transfer objects
- **Builder Pattern** - Dynamic SQL query construction
- **Strategy Pattern** - Different aggregation functions
- **Time-series Optimization** - TimescaleDB time_bucket() for efficient aggregation
- **Parameterized Queries** - SQL injection prevention
- **Connection Pooling** - HikariCP for database connections