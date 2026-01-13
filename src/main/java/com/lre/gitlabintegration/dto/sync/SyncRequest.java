package com.lre.gitlabintegration.dto.sync;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncRequest {

    private Long gitlabProjectId;
    private Long gitlabUserId;
    private String ref;
    private String lreDomain;
    private String lreProject;
}
