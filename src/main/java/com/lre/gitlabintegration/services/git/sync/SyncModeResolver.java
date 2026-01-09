package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncContext;
import com.lre.gitlabintegration.dto.sync.SyncRequest;
import com.lre.gitlabintegration.repository.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncModeResolver {

    private final GitRepositoryScanner scanner;
    private final SyncStateRepository stateRepository;

    public SyncContext resolve(SyncRequest syncRequest) {
        List<GitLabCommit> current =
                scanner.scanScripts(syncRequest.getGitlabProjectId(), syncRequest.getRef());
        List<GitLabCommit> previous =
                stateRepository.findPreviousCommits(syncRequest);

        boolean initial = previous.isEmpty();

        log.info("Resolved sync mode for project {}: {} sync (current={}, previous={})",
                syncRequest.getLreProject(),
                initial ? "INITIAL" : "INCREMENTAL",
                current.size(),
                previous.size()
        );

        return new SyncContext(syncRequest, previous, current, initial);
    }
}
