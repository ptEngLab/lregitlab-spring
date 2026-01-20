package com.lre.gitlabintegration.services.lre.workload;

import com.lre.gitlabintegration.dto.enums.common.StringValueEnum;
import com.lre.gitlabintegration.dto.lre.test.testcontent.workloadtype.WorkloadTypeDto;
import com.lre.gitlabintegration.util.text.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class WorkloadTypeFormatter {

    public String toKey(WorkloadTypeDto dto) {
        String type = safe(dto != null ? dto.getType() : null);
        String subType = safe(dto != null ? dto.getSubType() : null);

        return String.format("%s %s", type, subType)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public String toDisplayName(WorkloadTypeDto dto) {
        String type = safe(dto != null ? dto.getType() : null);
        String subType = safe(dto != null ? dto.getSubType() : null);
        String mode = dto != null && dto.getVusersDistributionMode() != null
                ? dto.getVusersDistributionMode().getValue()
                : null;

        String raw = StringUtils.isBlank(mode)
                ? String.format("%s %s", type, subType)
                : String.format("%s %s (%s)", type, subType, mode);

        return TextUtils.toTitleCase(raw);
    }

    private String safe(StringValueEnum e) {
        return (e == null || e.getValue() == null) ? "" : e.getValue();
    }
}
