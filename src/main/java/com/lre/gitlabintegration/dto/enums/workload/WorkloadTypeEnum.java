package com.lre.gitlabintegration.dto.enums.workload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;
import lombok.Getter;

@Getter
public enum WorkloadTypeEnum implements StringValueEnum {
    BASIC("basic"),
    REAL_WORLD("real-world"),
    GOAL_ORIENTED("goal oriented");

    private final String value;

    WorkloadTypeEnum(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static WorkloadTypeEnum fromValue(String value) {
        return StringValueEnum.fromValue(WorkloadTypeEnum.class, value);
    }
}
