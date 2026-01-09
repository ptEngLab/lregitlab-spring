package com.lre.gitlabintegration.dto.sync;

import lombok.Data;

@Data
public class SyncStateRow {
    private String sha = "";
    private String committedDate = "";
    private String path = "";
    private Integer lreScriptId;

}
