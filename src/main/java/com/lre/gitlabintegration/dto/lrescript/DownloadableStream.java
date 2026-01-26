package com.lre.gitlabintegration.dto.lrescript;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record DownloadableStream(StreamingResponseBody body, String fileName) {}
