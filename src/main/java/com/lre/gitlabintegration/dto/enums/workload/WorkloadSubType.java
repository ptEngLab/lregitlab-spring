package com.lre.gitlabintegration.dto.enums.workload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;
import lombok.Getter;

@Getter
public enum WorkloadSubType implements StringValueEnum {
    @JsonProperty("by test")
    BY_TEST("by test"),

    @JsonProperty("by group")
    BY_GROUP("by group");

    private final String value;
    
    WorkloadSubType(String value) {
        this.value = value;
    }

}
