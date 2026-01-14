package com.lre.gitlabintegration.services.security;


import com.lre.gitlabintegration.repository.LreAuthorizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LreAuthorizationService {

    private final LreAuthorizationRepository repo;

    public void assertUserHasProjectAccess(String gitlabUsername, String domain, String project) {
        if (!repo.isUserActiveAndHasAccess(gitlabUsername, domain, project)) {
            throw new AccessDeniedException("User is not authorized for this LRE project");
        }
    }
}
