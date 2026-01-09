package com.lre.gitlabintegration.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "lre")
public class LreProperties {

    @NotBlank
    private String url;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private boolean authenticateWithToken;

    @DurationMin(seconds = 1)
    @NotNull
    private Duration sessionTtl = Duration.ofMinutes(10);

    @Min(1)
    private int maxConsecutiveFailures = 3;

    @NotNull
    @DurationMin(seconds = 1)
    private Duration failureBackoff = Duration.ofMinutes(5);
}
