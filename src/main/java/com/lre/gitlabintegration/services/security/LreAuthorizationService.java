package com.lre.gitlabintegration.services.security;

import com.lre.gitlabintegration.repository.AllowedLreAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LreAuthorizationService {

    private final AllowedLreAccessRepository repo;

    public void assertAllowed(long gitlabProjectId, long gitlabUserId, String domain, String project) {
        if (domain == null || project == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path variables 'domain' and 'project' are required");
        }

        if (!repo.isAllowed(gitlabProjectId, gitlabUserId, domain, project)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed for this domain/project");
        }
    }
}
