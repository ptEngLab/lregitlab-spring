package com.lre.gitlabintegration.security;

import com.lre.gitlabintegration.client.api.GitLabJobTokenApiClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class GitLabCiJobTokenAuthFilter extends OncePerRequestFilter {

    private final GitLabJobTokenApiClient jobClient;

    public GitLabCiJobTokenAuthFilter(GitLabJobTokenApiClient jobClient) {
        this.jobClient = jobClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws IOException, ServletException {

        String token = request.getHeader("JOB-TOKEN");
        if (token == null || token.isBlank()) {
            response.sendError(401, "Missing JOB-TOKEN");
            return;
        }

        GitLabJobTokenApiClient.GitLabJobInfo job;
        try {
            job = jobClient.getCurrentJob(token);
        } catch (Exception ex) {
            response.sendError(401, "Invalid CI job token");
            return;
        }

        if (job == null || job.user() == null || job.project() == null) {
            response.sendError(401, "Could not resolve CI identity");
            return;
        }

        var principal = new GitLabCiPrincipal(
                job.project().id(),
                job.user().id(),
                job.user().username(),
                job.ref(),
                Boolean.TRUE.equals(job.tag())
        );

        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_CI")));

        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }

    public record GitLabCiPrincipal(long gitlabProjectId, long gitlabUserId, String username, String ref, boolean tag) {
    }

}
