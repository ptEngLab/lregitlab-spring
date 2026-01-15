package com.lre.gitlabintegration.dto.audit;

import com.lre.gitlabintegration.dto.gitlab.GitLabProjectInfo;
import com.lre.gitlabintegration.security.GitLabCiJobTokenAuthFilter;

public record AuditContext(
        GitLabCiJobTokenAuthFilter.GitLabCiPrincipal principal,
        GitLabProjectInfo projectInfo,
        String username,
        String domain,
        String project,
        int tag
) {}
