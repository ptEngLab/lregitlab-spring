package com.lre.gitlabintegration.dto.enums.rts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;

public enum HostType implements StringValueEnum {
    SPECIFIC("specific"),
    AUTOMATCH("automatch"),
    CLOUD("cloud"),
    DYNAMIC("dynamic");

    private final String value;

    HostType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static HostType fromValue(String value) {
        return StringValueEnum.fromValue(HostType.class, value);
    }
}
