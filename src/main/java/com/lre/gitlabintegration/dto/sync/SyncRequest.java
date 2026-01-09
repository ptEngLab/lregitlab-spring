package com.lre.gitlabintegration.dto.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncRequest {

    @NotNull(message = "GitLab project ID is required")
    private Integer gitlabProjectId;

    @NotBlank(message = "Git ref (branch/tag) is required")
    private String ref;

    @NotBlank(message = "LRE domain is required")
    private String lreDomain;

    @NotBlank(message = "LRE project is required")
    private String lreProject;
}
