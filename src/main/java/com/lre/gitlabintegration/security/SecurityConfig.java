package com.lre.gitlabintegration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lre.gitlabintegration.client.api.GitLabJobTokenApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            GitLabJobTokenApiClient jobClient,
                                            ObjectMapper objectMapper) {

        var entryPoint = new JsonAuthenticationEntryPoint(objectMapper);

        var p = PathPatternRequestMatcher.withDefaults();
        RequestMatcher publicEndpoints = new OrRequestMatcher(
                p.matcher("/health/**"),
                p.matcher("/actuator/health/**"),
                p.matcher("/error")
        );

        return http
                .csrf(AbstractHttpConfigurer::disable)

                // Explicitly disable default auth mechanisms (removes generated password usage paths)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(publicEndpoints).permitAll()
                        .anyRequest().hasAuthority("ROLE_GITLAB_CI")
                )
                .addFilterBefore(
                        new GitLabCiJobTokenAuthFilter(jobClient, entryPoint, publicEndpoints),
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    /**
     * Optional but recommended: prevents Spring Boot from auto-creating a default user/password.
     * Empty store since we authenticate only via JOB-TOKEN.
     */
    @Bean
    UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
