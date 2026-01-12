package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.LreScriptUrlFactory;
import com.lre.gitlabintegration.config.http.LreApiClientBaseRestApiClient;
import com.lre.gitlabintegration.dto.lrescript.Script;
import com.lre.gitlabintegration.dto.lrescript.ScriptUploadRequest;
import com.lre.gitlabintegration.exceptions.LreException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
public class LreScriptApiClient {

    private final LreApiClientBaseRestApiClient apiClient;
    private final LreScriptUrlFactory scriptUrlFactory;

    public LreScriptApiClient(
            LreApiClientBaseRestApiClient apiClient,
            LreScriptUrlFactory scriptUrlFactory
    ) {
        this.apiClient = apiClient;
        this.scriptUrlFactory = scriptUrlFactory;
    }

    public List<Script> getAllScripts(String domain, String project) {
        String url = scriptUrlFactory.getScriptsUrl(domain, project);
        log.debug("Fetching all scripts for {}/{}", domain, project);

        return apiClient.get(url, new ParameterizedTypeReference<>() {});
    }

    public Script uploadScript(
            String domain,
            String project,
            ScriptUploadRequest req,
            Path zipPath
    ) {
        String url = scriptUrlFactory.getScriptUploadUrl(domain, project);
        String fileName = zipPath.getFileName().toString();

        try {
            MultiValueMap<@NonNull String, Object> body = buildMultipartBody(req, zipPath, fileName);

            // BaseRestApiClient.postMultipart sets MULTIPART_FORM_DATA and expects JSON back
            Script script = apiClient.postMultipart(url, body, Script.class, null);

            validateUploadResponse(script, fileName);
            return script;

        } catch (IOException e) {
            log.error("I/O error reading script file: {}", fileName, e);
            throw new LreException("Failed to read script file: " + fileName, e);
        }
    }

    public Script getScriptById(String domain, String project, int scriptId) {
        String url = scriptUrlFactory.getScriptByIdUrl(domain, project, scriptId);
        log.debug("Fetching script by ID: {}", scriptId);

        return apiClient.get(url, Script.class);
    }

    public void deleteScript(String domain, String project, int scriptId) {
        String url = scriptUrlFactory.getScriptDeleteUrl(domain, project, scriptId);
        log.debug("Deleting script from LRE: {}", scriptId);

        apiClient.delete(url);
    }

    private MultiValueMap<@NonNull String, Object> buildMultipartBody(
            ScriptUploadRequest req,
            Path zipPath,
            String fileName
    ) throws IOException {

        byte[] zipBytes = Files.readAllBytes(zipPath);

        LinkedMultiValueMap<@NonNull String, @NonNull Object> body = new LinkedMultiValueMap<>();

        // Add metadata as JSON
        HttpHeaders metadataHeaders = new HttpHeaders();
        metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<@NonNull ScriptUploadRequest> metadataEntity = new HttpEntity<>(req, metadataHeaders);
        body.add("metadata", metadataEntity);

        // Add file as binary
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        ByteArrayResource fileResource = new ByteArrayResource(zipBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        HttpEntity<@NonNull ByteArrayResource> fileEntity = new HttpEntity<>(fileResource, fileHeaders);
        body.add("file", fileEntity);

        return body;
    }

    private void validateUploadResponse(Script script, String fileName) {
        if (script == null) {
            throw new LreException("LRE returned null response for script upload: " + fileName);
        }
        if (script.getId() == null) {
            throw new LreException("LRE returned script without ID for: " + fileName);
        }
    }
}
