package com.lre.gitlabintegration.dto.enums.workload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;
import lombok.Getter;

@Getter
public enum WorkloadVusersDistributionMode implements StringValueEnum{
    BY_NUMBER("by number"),
    BY_PERCENTAGE("by percentage");

    private final String value;

    WorkloadVusersDistributionMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static WorkloadVusersDistributionMode fromValue(String value) {
        return StringValueEnum.fromValue(WorkloadVusersDistributionMode.class, value);
    }
}
