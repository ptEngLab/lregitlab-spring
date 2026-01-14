package com.lre.gitlabintegration.dto.lreuser;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LreUserRoleDto(
        @JsonProperty("Domain") String domain,
        @JsonProperty("ProjectName") String projectName,
        @JsonProperty("ProjectID") String projectId,
        @JsonProperty("Role") String role
) {}
