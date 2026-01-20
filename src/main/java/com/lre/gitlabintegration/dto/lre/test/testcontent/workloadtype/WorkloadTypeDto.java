package com.lre.gitlabintegration.dto.lre.test.testcontent.workloadtype;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.lre.gitlabintegration.dto.enums.workload.WorkloadSubType;
import com.lre.gitlabintegration.dto.enums.workload.WorkloadTypeEnum;
import com.lre.gitlabintegration.dto.enums.workload.WorkloadVusersDistributionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.lre.gitlabintegration.util.constants.AppConstants.LRE_API_XML_NS;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkloadTypeDto {

    @JsonProperty("Type")
    @JacksonXmlProperty(localName = "Type", namespace = LRE_API_XML_NS)
    private WorkloadTypeEnum type;

    @JsonProperty("SubType")
    @JacksonXmlProperty(localName = "SubType", namespace = LRE_API_XML_NS)
    private WorkloadSubType subType;

    @JsonProperty("VusersDistributionMode")
    @JacksonXmlProperty(localName = "VusersDistributionMode", namespace = LRE_API_XML_NS)
    private WorkloadVusersDistributionMode vusersDistributionMode;
}
