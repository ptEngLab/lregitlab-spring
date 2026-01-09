package com.lre.gitlabintegration.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitLabCommit {

    @JsonProperty("id")
    private String sha = "";

    @JsonProperty("committed_date")
    private String committedDate = "";

    @JsonProperty("path")
    private String path = "";

    @JsonProperty("message")
    private String message = "";

    @JsonIgnore
    public boolean isEmpty() {
        return sha.isBlank() && committedDate.isBlank();
    }
}
