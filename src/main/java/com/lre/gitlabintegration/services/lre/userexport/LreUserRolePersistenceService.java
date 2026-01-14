package com.lre.gitlabintegration.services.lre.userexport;

import com.lre.gitlabintegration.dto.lreuser.LreUserDto;
import com.lre.gitlabintegration.repository.LreSyncRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LreUserRolePersistenceService {

    private final LreSyncRepository repo;

    @Transactional
    public void persistUsersAndRoles(List<LreUserDto> users) {
        repo.upsertUsers(users);
        repo.replaceAllRoles(LreSyncRepository.flattenRoles(users));
    }
}
