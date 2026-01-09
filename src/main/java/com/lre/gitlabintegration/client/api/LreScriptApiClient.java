package com.lre.gitlabintegration.client.api;

import com.lre.gitlabintegration.client.builder.LreScriptUrlFactory;
import com.lre.gitlabintegration.dto.lrescript.Script;
import com.lre.gitlabintegration.dto.lrescript.ScriptUploadRequest;
import com.lre.gitlabintegration.exceptions.LreException;
import com.lre.gitlabintegration.util.http.HttpErrorHandler;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class LreScriptApiClient {

    private final RestClient lreRestClient;
    private final LreScriptUrlFactory scriptUrlFactory;

    public LreScriptApiClient(
            @Qualifier("lreRestClient") RestClient lreRestClient,
            LreScriptUrlFactory scriptUrlFactory
    ) {
        this.lreRestClient = lreRestClient;
        this.scriptUrlFactory = scriptUrlFactory;
    }

    public List<Script> getAllScripts(String domain, String project) {
        String url = scriptUrlFactory.getScriptsUrl(domain, project);
        log.debug("Fetching all scripts for {}/{}", domain, project);

        try {
            List<Script> scripts = lreRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return scripts != null ? scripts : Collections.emptyList();
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(e, url, "Fetching scripts for project: " + domain + "/" + project);
            return Collections.emptyList(); // never reached, but satisfies compiler
        }
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
            MultiValueMap<@NonNull String, Object> body =
                    buildMultipartBody(req, zipPath, fileName);

            Script script = lreRestClient.post()
                    .uri(url)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Script.class);

            validateUploadResponse(script, fileName);
            return script;
        } catch (IOException e) {
            log.error("I/O error reading script file: {}", fileName, e);
            throw new LreException("Failed to read script file: " + fileName, e);
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(e, url, "Uploading script for project: " + domain + "/" + project);
            throw new IllegalStateException("This should never be reached, handleRestClientError always throws an exception");
        }
    }

    private MultiValueMap<@NonNull String, Object> buildMultipartBody(
            ScriptUploadRequest req,
            Path zipPath,
            String fileName
    ) throws IOException {

        byte[] zipBytes = Files.readAllBytes(zipPath);

        LinkedMultiValueMap<@NonNull String, @NonNull Object> body =
                new LinkedMultiValueMap<>();

        // Add metadata as JSON
        HttpHeaders metadataHeaders = new HttpHeaders();
        metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<@NonNull ScriptUploadRequest> metadataEntity =
                new HttpEntity<>(req, metadataHeaders);
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

        HttpEntity<@NonNull ByteArrayResource> fileEntity =
                new HttpEntity<>(fileResource, fileHeaders);
        body.add("file", fileEntity);

        return body;
    }

    private void validateUploadResponse(Script script, String fileName) {
        if (script == null) throw new LreException("LRE returned null response for script upload: " + fileName);
        if (script.getId() == null) throw new LreException("LRE returned script without ID for: " + fileName);

    }

    public Script getScriptById(String domain, String project, int scriptId) {
        String url = scriptUrlFactory.getScriptByIdUrl(domain, project, scriptId);
        log.debug("Fetching script by ID: {}", scriptId);

        try {
            return lreRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Script.class);
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(e, url, "Fetching script by ID: " + scriptId);
            return null;
        }
    }

    public void deleteScript(String domain, String project, int scriptId) {
        String url = scriptUrlFactory.getScriptDeleteUrl(domain, project, scriptId);
        log.debug("Deleting script from LRE: {}", scriptId);

        try {
            lreRestClient.delete()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            HttpErrorHandler.handleRestClientError(e, url, "Deleting script ID: " + scriptId);
        }
    }
}
