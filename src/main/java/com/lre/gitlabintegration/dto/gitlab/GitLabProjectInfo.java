package com.lre.gitlabintegration.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitLabProjectInfo(
        long id,
        String name,
        @JsonProperty("path_with_namespace") String pathWithNamespace,
        @JsonProperty("web_url") String webUrl
) {}
