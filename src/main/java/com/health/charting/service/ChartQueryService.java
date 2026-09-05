package com.health.charting.service;

import com.health.charting.dto.request.ChartQueryRequest;
import com.health.charting.dto.response.ChartResponse;

/**
 * Service interface for querying chart data from TimescaleDB.
 * Provides methods to execute chart queries and return formatted responses.
 */
public interface ChartQueryService {
    
    /**
     * Execute a chart query and return formatted chart data.
     * 
     * @param request the chart query request containing all query parameters
     * @return ChartResponse containing all series data
     * @throws com.health.charting.exception.InvalidQueryException if the query is invalid
     * @throws com.health.charting.exception.DataNotFoundException if no data is found
     */
    ChartResponse query(ChartQueryRequest request);
}

// Made with Bob
