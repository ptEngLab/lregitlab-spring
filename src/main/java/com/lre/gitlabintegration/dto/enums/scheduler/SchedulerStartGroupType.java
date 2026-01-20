package com.lre.gitlabintegration.dto.enums.scheduler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;

public enum SchedulerStartGroupType implements StringValueEnum {
    IMMEDIATELY("immediately"),
    WITH_DELAY("with delay"),
    WHEN_GROUP_FINISHES("when group finishes");

    private final String value;

    SchedulerStartGroupType(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static SchedulerStartGroupType fromValue(String value) {
        return StringValueEnum.fromValue(SchedulerStartGroupType.class, value);
    }
}
