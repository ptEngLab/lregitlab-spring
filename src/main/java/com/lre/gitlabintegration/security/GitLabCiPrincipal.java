package com.lre.gitlabintegration.security;

public record GitLabCiPrincipal(
        long gitlabUserId,
        String gitlabUsername,
        long gitlabProjectId,
        String webUrl,
        String ref,
        boolean tag
) {
}
