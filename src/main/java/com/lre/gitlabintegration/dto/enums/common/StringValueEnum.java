package com.lre.gitlabintegration.dto.enums.common;

import com.fasterxml.jackson.annotation.JsonValue;

public interface StringValueEnum {

    @JsonValue
    String getValue();

    static <E extends Enum<E> & StringValueEnum> E fromValue(Class<E> enumClass, String value) {
        if (value == null) throw new IllegalArgumentException(enumClass.getSimpleName() + " value cannot be null");
        for (E enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.getValue().equalsIgnoreCase(value)) return enumConstant;
        }
        throw new IllegalArgumentException("Unknown " + enumClass.getSimpleName() + ": " + value);
    }
}
