package com.lre.gitlabintegration.dto.enums.scheduler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;

public enum SchedulerInitializeType implements StringValueEnum {
    GRADUALLY("gradually"),
    JUST_BEFORE_VUSER_RUNS("just before vuser runs"),
    SIMULTANEOUSLY("simultaneously");

    private final String value;

    SchedulerInitializeType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SchedulerInitializeType fromValue(String value) {
        return StringValueEnum.fromValue(SchedulerInitializeType.class, value);
    }
}
