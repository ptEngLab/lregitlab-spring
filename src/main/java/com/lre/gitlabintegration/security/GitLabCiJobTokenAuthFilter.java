package com.lre.gitlabintegration.security;

import com.lre.gitlabintegration.client.api.GitLabJobTokenApiClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws IOException, ServletException {

        String token = request.getHeader("JOB-TOKEN");
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Missing JOB-TOKEN");
        }

        GitLabJobTokenApiClient.GitLabJobInfo job;
        try {
            job = jobClient.getCurrentJob(token);
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid CI Job token", ex);
        }

        var auth = getToken(job);

        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private static UsernamePasswordAuthenticationToken getToken(GitLabJobTokenApiClient.GitLabJobInfo job) {
        if (job == null || job.user() == null || job.project() == null) {
            throw new BadCredentialsException("Could not resolve CI identity");
        }

        var principal = new GitLabCiPrincipal(
                job.project().id(),
                job.user().id(),
                job.user().username(),
                job.ref(),
                Boolean.TRUE.equals(job.tag())
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CI"))
        );

    }

    public record GitLabCiPrincipal(long gitlabProjectId, long gitlabUserId, String username, String ref, boolean tag) {}
}
