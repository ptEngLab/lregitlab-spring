package com.lre.gitlabintegration.dto.lreauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {

    @JsonProperty("ClientIdKey")
    private String username;

    @JsonProperty("ClientSecretKey")
    private String password;
}
