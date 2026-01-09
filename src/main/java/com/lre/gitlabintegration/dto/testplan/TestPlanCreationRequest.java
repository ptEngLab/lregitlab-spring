package com.lre.gitlabintegration.dto.testplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TestPlanCreationRequest {

    @JsonProperty("Path")
    private String path;

    @JsonProperty("Name")
    private String name;
}
