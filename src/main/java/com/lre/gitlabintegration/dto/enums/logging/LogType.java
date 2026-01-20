package com.lre.gitlabintegration.dto.enums.logging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;

public enum LogType implements StringValueEnum {
    IGNORE("ignore"),
    STANDARD("standard"),
    EXTENDED("extended"),
    DISABLE("disable");

    private final String value;

    LogType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LogType fromValue(String value) {
        return StringValueEnum.fromValue(LogType.class, value);
    }
}
