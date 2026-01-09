package com.lre.gitlabintegration.dto.lrescript;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScriptUploadRequest {

    @JsonProperty("TestFolderPath")
    private String testFolderPath;

    @JsonProperty("Overwrite")
    private boolean overwrite = true;

    @JsonProperty("RuntimeOnly")
    private boolean runtimeOnly = true;

    @JsonProperty("KeepCheckedOut")
    private boolean keepCheckedOut = false;

    public ScriptUploadRequest(String testFolderPath) {
        this.testFolderPath = testFolderPath;
    }
}
