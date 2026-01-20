package com.lre.gitlabintegration.dto.enums.sla;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;

public enum SlaLoadCriteria implements StringValueEnum {
    NONE("None"),
    RUNNING_VUSERS("running_vusers"),
    THROUGHPUT("throughput"),
    HITS_PER_SECOND("hits_per_second"),
    TRANSACTIONS_PER_SECOND("transactions_per_second"),
    TRANSACTIONS_PER_SECOND_PASSED("transactions_per_second_passed");

    private final String value;

    SlaLoadCriteria(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SlaLoadCriteria fromValue(String value) {
        return StringValueEnum.fromValue(SlaLoadCriteria.class, value);
    }
}
