package com.lre.gitlabintegration.dto.enums.scheduler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;

public enum SchedulerDurationType implements StringValueEnum {
    INDEFINITELY("indefinitely"),
    RUN_FOR("run for"),
    UNTIL_COMPLETION("until completion");

    private final String value;

    SchedulerDurationType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static SchedulerDurationType fromValue(String value) {
        return StringValueEnum.fromValue(SchedulerDurationType.class, value);
    }
}
