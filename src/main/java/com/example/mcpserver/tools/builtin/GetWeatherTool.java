package com.example.mcpserver.tools.builtin;

import com.example.mcpserver.tool.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Tool for fetching weather information for a given location.
 * Returns weather data including temperature, conditions, and other meteorological information.
 */
@Component
public class GetWeatherTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(GetWeatherTool.class);

    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    
    private static final String SCHEMA = """
        {
            "$schema": "http://json-schema.org/draft-07/schema#",
            "type": "object",
            "description": "Fetches the weather in the given location",
            "properties": {
                "location": {
                    "type": "string",
                    "description": "The location to get the weather for"
                },
                "unit": {
                    "type": "string",
                    "description": "The unit to return the temperature in",
                    "enum": ["F", "C"]
                }
            },
            "additionalProperties": false,
            "required": ["location", "unit"]
        }
        """;
    
    // Sample weather conditions for demonstration
    private static final String[] WEATHER_CONDITIONS = {
        "Sunny", "Partly Cloudy", "Cloudy", "Overcast", "Light Rain", 
        "Heavy Rain", "Thunderstorms", "Snow", "Fog", "Clear"
    };
    
    @Autowired
    public GetWeatherTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String getName() {
        return "get_weather";
    }
    
    @Override
    public String getDescription() {
        return "Fetches the weather in the given location";
    }
    
    @Override
    public String getSchema() {
        return SCHEMA;
    }
    
    @Override
    public ToolMetadata getMetadata() {
        return ToolMetadata.builder()
                .name(getName())
                .description(getDescription())
                .version("1.0.0")
                .category("weather")
                .schema(getSchema())
                .build();
    }
    
    @Override
    public ValidationResult validate(Map<String, Object> arguments) {
        logger.debug("Validating get_weather arguments: {}", arguments.keySet());

        if (arguments == null || arguments.isEmpty()) {
            logger.warn("Validation failed: no arguments provided");
            return ValidationResult.failure("Arguments are required. Please provide 'location' and 'unit' parameters.");
        }

        // Check for location parameter
        if (!arguments.containsKey("location") || arguments.get("location") == null) {
            logger.warn("Validation failed: missing 'location' parameter");
            return ValidationResult.failure("'location' parameter is required.");
        }

        Object locationObj = arguments.get("location");
        if (!(locationObj instanceof String)) {
            logger.warn("Validation failed: 'location' must be a string");
            return ValidationResult.failure("'location' must be a string.");
        }

        String location = (String) locationObj;
        if (location.trim().isEmpty()) {
            logger.warn("Validation failed: 'location' cannot be empty");
            return ValidationResult.failure("'location' cannot be empty.");
        }

        // Check for unit parameter
        if (!arguments.containsKey("unit") || arguments.get("unit") == null) {
            logger.warn("Validation failed: missing 'unit' parameter");
            return ValidationResult.failure("'unit' parameter is required.");
        }

        Object unitObj = arguments.get("unit");
        if (!(unitObj instanceof String)) {
            logger.warn("Validation failed: 'unit' must be a string");
            return ValidationResult.failure("'unit' must be a string.");
        }

        String unit = (String) unitObj;
        if (!unit.equals("F") && !unit.equals("C")) {
            logger.warn("Validation failed: invalid unit '{}', must be 'F' or 'C'", unit);
            return ValidationResult.failure("'unit' must be either 'F' (Fahrenheit) or 'C' (Celsius).");
        }

        logger.debug("Validation successful for get_weather with location: '{}', unit: '{}'", location, unit);
        return ValidationResult.success();
    }

    @Override
    public Mono<ToolResult> execute(McpAsyncServerExchange exchange, ToolContext context, Map<String, Object> arguments) {
        logger.info("Executing get_weather tool for session: {}", context.getSessionId());

        return Mono.fromCallable(() -> {
            // Extract parameters from arguments
            String location = (String) arguments.get("location");
            String unit = (String) arguments.get("unit");
            
            logger.debug("Fetching weather for location: '{}' in unit: '{}'", location, unit);

            // Generate mock weather data (in a real implementation, this would call a weather API)
            Map<String, Object> weatherData = generateMockWeatherData(location, unit);
            
            logger.info("Successfully fetched weather data for location: '{}'", location);
            logger.debug("Weather data: {}", weatherData);

            // Format successful response
            Map<String, Object> response = Map.of(
                "location", location,
                "unit", unit,
                "weather", weatherData
            );

            String responseJson = objectMapper.writeValueAsString(response);
            
            logger.debug("Returning weather response for location: '{}'", location);
            return ToolResult.success(List.of(new McpSchema.TextContent(responseJson)));
        })
        .doOnError(error -> logger.error("Failed to fetch weather data: {}", error.getMessage(), error))
        .onErrorResume(error -> Mono.just(ToolResult.error("Failed to fetch weather data: " + error.getMessage())));
    }

    /**
     * Generates mock weather data for demonstration purposes.
     * In a real implementation, this would call an external weather API.
     *
     * @param location the location to get weather for
     * @param unit the temperature unit (F or C)
     * @return mock weather data
     */
    private Map<String, Object> generateMockWeatherData(String location, String unit) {
        // Generate random temperature based on unit
        int temperature;
        if ("F".equals(unit)) {
            temperature = random.nextInt(80) + 20; // 20-100°F
        } else {
            temperature = random.nextInt(35) - 5; // -5 to 30°C
        }
        
        // Random weather condition
        String condition = WEATHER_CONDITIONS[random.nextInt(WEATHER_CONDITIONS.length)];
        
        // Random humidity and wind speed
        int humidity = random.nextInt(60) + 30; // 30-90%
        int windSpeed = random.nextInt(20) + 5; // 5-25 mph/kmh
        
        return Map.of(
            "temperature", temperature,
            "condition", condition,
            "humidity", humidity + "%",
            "wind_speed", windSpeed + (unit.equals("F") ? " mph" : " km/h"),
            "feels_like", temperature + random.nextInt(6) - 3, // ±3 degrees
            "visibility", random.nextInt(5) + 5 + (unit.equals("F") ? " miles" : " km"),
            "timestamp", java.time.Instant.now().toString()
        );
    }
}
