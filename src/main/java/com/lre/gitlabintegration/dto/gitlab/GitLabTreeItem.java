package com.lre.gitlabintegration.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabTreeItem {

    private String id;
    private String name;
    private String type; // "tree" or "blob"
    private String path;
    private String mode;

    @JsonProperty("ref")
    private String branch;
}
