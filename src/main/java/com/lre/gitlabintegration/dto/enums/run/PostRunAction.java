package com.lre.gitlabintegration.dto.enums.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;
import lombok.Getter;

import java.util.*;

@Getter
public enum PostRunAction implements StringValueEnum {

    COLLATE_AND_ANALYZE(
            "Collate and Analyze",
            2,
            EnumSet.of(
                    RunState.FINISHED,
                    RunState.FAILED_COLLATING_RESULTS,
                    RunState.FAILED_CREATING_ANALYSIS_DATA,
                    RunState.RUN_FAILURE,
                    RunState.CANCELED
            )
    ),

    COLLATE(
            "Collate Results",
            1,
            EnumSet.of(
                    RunState.BEFORE_CREATING_ANALYSIS_DATA,
                    RunState.FAILED_COLLATING_RESULTS,
                    RunState.RUN_FAILURE,
                    RunState.CANCELED
            )
    ),

    DO_NOTHING(
            "Do Not Collate",
            0,
            EnumSet.of(
                    RunState.BEFORE_COLLATING_RESULTS,
                    RunState.RUN_FAILURE,
                    RunState.CANCELED
            )
    );

    private static final Map<String, PostRunAction> VALUE_LOOKUP;
    private static final Map<Integer, PostRunAction> NUMERIC_LOOKUP;

    static {
        Map<String, PostRunAction> valueMap = new HashMap<>();
        Map<Integer, PostRunAction> numericMap = new HashMap<>();

        for (PostRunAction action : values()) {
            valueMap.put(normalize(action.value), action);          // display value
            valueMap.put(normalize(action.name()), action);         // enum name alias
            numericMap.put(action.numericValue, action);
        }

        VALUE_LOOKUP = Collections.unmodifiableMap(valueMap);
        NUMERIC_LOOKUP = Collections.unmodifiableMap(numericMap);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private final String value;
    private final int numericValue;
    private final Set<RunState> terminalStates;

    PostRunAction(String value, int numericValue, EnumSet<RunState> terminalStates) {
        this.value = value;
        this.numericValue = numericValue;
        this.terminalStates = Collections.unmodifiableSet(EnumSet.copyOf(terminalStates));
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PostRunAction fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("PostRunAction value cannot be null or empty");
        }

        PostRunAction action = VALUE_LOOKUP.get(normalize(value));
        if (action == null) {
            throw new IllegalArgumentException("Unknown PostRunAction: " + value);
        }
        return action;
    }

    /**
     * Lookup by numeric value.
     */
    public static PostRunAction fromNumericValue(int numericValue) {
        PostRunAction action = NUMERIC_LOOKUP.get(numericValue);
        if (action == null) {
            throw new IllegalArgumentException("Unknown numeric PostRunAction: " + numericValue);
        }
        return action;
    }

    /**
     * Check if a given RunState is terminal for this PostRunAction.
     */
    public boolean isTerminal(RunState state) {
        return state != null && terminalStates.contains(state);
    }

    @Override
    public String toString() {
        return value;
    }
}
