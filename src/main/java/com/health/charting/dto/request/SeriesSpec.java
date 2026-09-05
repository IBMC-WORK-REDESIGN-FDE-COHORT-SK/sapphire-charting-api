package com.health.charting.dto.request;

import com.health.charting.enums.Aggregation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Specification for a single data series in the chart query.
 * Defines what metric to query and how to aggregate it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Specification for a data series")
public class SeriesSpec {
    
    @NotBlank(message = "Series name is required")
    @Schema(description = "Display name for this series", example = "Heart Rate", required = true)
    private String name;
    
    @NotBlank(message = "Metric name is required")
    @Schema(description = "Specific metric to query (e.g., 'heartrate', 'systolic', 'steps')", 
            example = "heartrate", required = true)
    private String metricName;
    
    @Schema(description = "JSONB attribute filters for this series", 
            example = "{\"device_type\": \"Apple Watch\", \"position\": \"left_wrist\"}")
    @Builder.Default
    private Map<String, String> attributeFilter = new HashMap<>();
    
    @Schema(description = "Aggregation function for this series (overrides request-level aggregation)", 
            example = "AVG")
    private Aggregation aggregation;
    
    /**
     * Check if this series has attribute filters.
     * 
     * @return true if filters exist, false otherwise
     */
    public boolean hasAttributeFilters() {
        return attributeFilter != null && !attributeFilter.isEmpty();
    }
    
    /**
     * Add an attribute filter.
     * 
     * @param key the attribute key
     * @param value the attribute value
     */
    public void addAttributeFilter(String key, String value) {
        if (this.attributeFilter == null) {
            this.attributeFilter = new HashMap<>();
        }
        this.attributeFilter.put(key, value);
    }
}

// Made with Bob
