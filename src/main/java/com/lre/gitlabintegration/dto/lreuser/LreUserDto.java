package com.lre.gitlabintegration.dto.lreuser;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LreUserDto(
        @JsonProperty("ID") long id,
        @JsonProperty("UserName") String userName,
        @JsonProperty("FullName") String fullName,
        @JsonProperty("Status") String status,
        @JsonProperty("LastUpdateDate") String lastUpdateDate,
        @JsonProperty("Email") String email,
        @JsonProperty("AdditionalData") List<LreAdditionalDataDto> additionalData
) {}
