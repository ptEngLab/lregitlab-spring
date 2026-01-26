package com.lre.gitlabintegration.security;

import com.lre.gitlabintegration.client.api.GitLabJobTokenApiClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class GitLabCiJobTokenAuthFilter extends OncePerRequestFilter {

    private static final String JOB_TOKEN_HEADER = "JOB-TOKEN";
    private static final String ROLE_GITLAB_CI = "ROLE_GITLAB_CI";

    private final GitLabJobTokenApiClient jobClient;
    private final AuthenticationEntryPoint entryPoint;
    private final RequestMatcher skipMatcher;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || skipMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws IOException, ServletException {

        try {
            var existing = SecurityContextHolder.getContext().getAuthentication();
            if (existing != null
                    && !(existing instanceof AnonymousAuthenticationToken)
                    && existing.isAuthenticated()) {
                chain.doFilter(request, response);
                return;
            }

            String token = request.getHeader(JOB_TOKEN_HEADER);
            if (token == null || token.isBlank()) {
                throw new BadCredentialsException("Missing JOB-TOKEN");
            }

            var job = resolveJob(token);
            var auth = buildAuthentication(job);

            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);

        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            entryPoint.commence(request, response, ex);
        }
    }

    private GitLabJobTokenApiClient.GitLabJobInfo resolveJob(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(JOB_TOKEN_HEADER, token);
            return jobClient.getCurrentJob(headers);
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid CI Job token", ex);
        }
    }

    private static UsernamePasswordAuthenticationToken buildAuthentication(GitLabJobTokenApiClient.GitLabJobInfo job) {
        if (job == null || job.user() == null || job.pipeline() == null) {
            throw new BadCredentialsException("Could not resolve CI identity");
        }

        var principal = new GitLabCiPrincipal(
                job.user().id(),
                job.user().username(),
                job.pipeline().projectId(),
                job.pipeline().webUrl(),
                job.ref(),
                Boolean.TRUE.equals(job.tag())
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(ROLE_GITLAB_CI))
        );
    }

}
