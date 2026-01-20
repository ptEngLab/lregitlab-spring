package com.lre.gitlabintegration.dto.enums.rts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;
import lombok.Getter;

@Getter
public enum LGDistributionType implements StringValueEnum {
    ALL_TO_EACH_GROUP("all to each group"),
    MANUAL("manual");

    private final String value;

    LGDistributionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LGDistributionType fromValue(String value) {
        return StringValueEnum.fromValue(LGDistributionType.class, value);
    }
}
