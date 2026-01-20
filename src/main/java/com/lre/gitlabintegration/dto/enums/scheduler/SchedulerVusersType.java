package com.lre.gitlabintegration.dto.enums.scheduler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;

/* Both StartVusers and StopVusers share the same type options: */

public enum SchedulerVusersType implements StringValueEnum {
    SIMULTANEOUSLY("simultaneously"),
    GRADUALLY("gradually");

    private final String value;

    SchedulerVusersType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static SchedulerVusersType fromValue(String value) {
        return StringValueEnum.fromValue(SchedulerVusersType.class, value);
    }
}
