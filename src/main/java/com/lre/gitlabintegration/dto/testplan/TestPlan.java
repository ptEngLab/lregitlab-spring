package com.lre.gitlabintegration.dto.testplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TestPlan {

    @JsonProperty("Id")
    private int id;

    @JsonProperty("ParentId")
    private int parentId;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("FullPath")
    private String fullPath;
}
