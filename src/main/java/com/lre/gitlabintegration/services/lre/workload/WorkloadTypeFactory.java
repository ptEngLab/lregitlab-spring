package com.lre.gitlabintegration.services.lre.workload;

import com.lre.gitlabintegration.dto.enums.workload.WorkloadSubType;
import com.lre.gitlabintegration.dto.enums.workload.WorkloadTypeEnum;
import com.lre.gitlabintegration.dto.enums.workload.WorkloadVusersDistributionMode;
import com.lre.gitlabintegration.dto.lre.test.testcontent.workloadtype.WorkloadTypeDto;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.lre.gitlabintegration.dto.enums.workload.WorkloadTypeEnum.BASIC;
import static com.lre.gitlabintegration.dto.enums.workload.WorkloadTypeEnum.REAL_WORLD;

@Component
public class WorkloadTypeFactory {

    private static final WorkloadTypeDto DEFAULT = WorkloadTypeDto.builder()
            .type(BASIC)
            .subType(WorkloadSubType.BY_TEST)
            .vusersDistributionMode(WorkloadVusersDistributionMode.BY_NUMBER)
            .build();

    private static final Map<Integer, WorkloadTypeDto> CODE_MAP = Map.of(
            1, WorkloadTypeDto.builder()
                    .type(BASIC).subType(WorkloadSubType.BY_TEST)
                    .vusersDistributionMode(WorkloadVusersDistributionMode.BY_NUMBER)
                    .build(),
            2, WorkloadTypeDto.builder()
                    .type(BASIC).subType(WorkloadSubType.BY_TEST)
                    .vusersDistributionMode(WorkloadVusersDistributionMode.BY_PERCENTAGE)
                    .build(),
            3, WorkloadTypeDto.builder()
                    .type(BASIC).subType(WorkloadSubType.BY_GROUP)
                    .build(),
            4, WorkloadTypeDto.builder()
                    .type(REAL_WORLD).subType(WorkloadSubType.BY_TEST)
                    .vusersDistributionMode(WorkloadVusersDistributionMode.BY_NUMBER)
                    .build(),
            5, WorkloadTypeDto.builder()
                    .type(REAL_WORLD).subType(WorkloadSubType.BY_TEST)
                    .vusersDistributionMode(WorkloadVusersDistributionMode.BY_PERCENTAGE)
                    .build(),
            6, WorkloadTypeDto.builder()
                    .type(REAL_WORLD).subType(WorkloadSubType.BY_GROUP)
                    .build(),
            7, WorkloadTypeDto.builder()
                    .type(WorkloadTypeEnum.GOAL_ORIENTED)
                    .build()
    );

    public WorkloadTypeDto fromUserInput(Integer userInput) {
        if (userInput == null) {
            return DEFAULT;
        }
        WorkloadTypeDto mapped = CODE_MAP.get(userInput);
        if (mapped == null) {
            throw new IllegalArgumentException("Invalid WorkloadType code: " + userInput);
        }
        return mapped;
    }
}
