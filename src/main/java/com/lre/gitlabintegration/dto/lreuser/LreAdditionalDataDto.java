package com.lre.gitlabintegration.dto.lreuser;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LreAdditionalDataDto(
        @JsonProperty("UserRoles") List<LreUserRoleDto> userRoles
) {}
