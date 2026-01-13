package com.lre.gitlabintegration.security;

import com.lre.gitlabintegration.client.api.GitLabJobTokenApiClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class GitLabCiJobTokenAuthFilter extends OncePerRequestFilter {

    private static final String JOB_TOKEN_HEADER = "JOB-TOKEN";

    private final GitLabJobTokenApiClient jobClient;
    private final AuthenticationEntryPoint entryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "GET".equals(request.getMethod())
                && (path.startsWith("/health") || path.startsWith("/error"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws IOException, ServletException {

        try {
            String token = request.getHeader(JOB_TOKEN_HEADER);
            if (token == null || token.isBlank()) {
                throw new BadCredentialsException("Missing JOB-TOKEN");
            }

            var job = resolveJob(token);
            var auth = getToken(job);

            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);

        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, ex);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }


    private GitLabJobTokenApiClient.GitLabJobInfo resolveJob(String token) {
        try {
            return jobClient.getCurrentJob(token);
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid CI Job token", ex);
        }
    }

    private static UsernamePasswordAuthenticationToken getToken(
            GitLabJobTokenApiClient.GitLabJobInfo job
    ) {
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
                List.of(new SimpleGrantedAuthority("ROLE_GITLAB_CI"))
        );
    }

    public record GitLabCiPrincipal(
            long gitlabProjectId,
            long gitlabUserId,
            String username,
            String ref,
            boolean tag
    ) {}
}
