package com.lre.gitlabintegration.dto.enums.rts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum PacingStartNewIterationType {

    IMMEDIATELY("immediately", ConfigShape.NONE),
    FIXED_DELAY("fixed delay", ConfigShape.FIXED),
    RANDOM_DELAY("random delay", ConfigShape.RANGE),
    FIXED_INTERVAL("fixed interval", ConfigShape.FIXED),
    RANDOM_INTERVAL("random interval", ConfigShape.RANGE);

    private final String value;
    private final ConfigShape configShape;

    PacingStartNewIterationType(String value, ConfigShape configShape) {
        this.value = value;
        this.configShape = configShape;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PacingStartNewIterationType fromString(String input) {
        return Arrays.stream(values())
                .filter(t -> t.value.equalsIgnoreCase(input.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid pacing type: " + input));
    }

    public enum ConfigShape {
        NONE,   // immediately
        FIXED,  // fixed delay/interval
        RANGE   // random delay/interval
    }
}
