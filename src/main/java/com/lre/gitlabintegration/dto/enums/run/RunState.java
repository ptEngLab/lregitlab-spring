package com.lre.gitlabintegration.dto.enums.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;

public enum RunState implements StringValueEnum {
    UNDEFINED(""),
    INITIALIZING("Initializing"),
    RUNNING("Running"),
    STOPPING("Stopping"),
    BEFORE_COLLATING_RESULTS("Before Collating Results"),
    COLLATING_RESULTS("Collating Results"),
    BEFORE_CREATING_ANALYSIS_DATA("Before Creating Analysis Data"),
    PENDING_CREATING_ANALYSIS_DATA("Pending Creating Analysis Data"),
    CREATING_ANALYSIS_DATA("Creating Analysis Data"),
    FINISHED("Finished"),
    FAILED_COLLATING_RESULTS("Failed Collating Results"),
    FAILED_CREATING_ANALYSIS_DATA("Failed Creating Analysis Data"),
    CANCELED("Canceled"),
    RUN_FAILURE("Run Failure");

    private final String value;

    RunState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RunState fromValue(String value) {
        return StringValueEnum.fromValue(RunState.class, value);
    }
}
