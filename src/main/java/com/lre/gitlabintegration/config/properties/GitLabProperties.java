package com.lre.gitlabintegration.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "gitlab")
public class GitLabProperties {
    private String url;
    private String token;
    private int perPageRecords;
    private int threadPoolSize;

}
