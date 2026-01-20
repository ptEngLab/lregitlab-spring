package com.lre.gitlabintegration.dto.enums.logging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;

public enum LogOptionsType implements StringValueEnum {
    ON_ERROR("on error"),
    ALWAYS("always");

    private final String value;

    LogOptionsType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LogOptionsType fromValue(String value) {
        return StringValueEnum.fromValue(LogOptionsType.class, value);
    }
}
