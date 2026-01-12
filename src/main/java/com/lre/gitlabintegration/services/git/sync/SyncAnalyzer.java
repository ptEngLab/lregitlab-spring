package com.lre.gitlabintegration.services.git.sync;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import com.lre.gitlabintegration.dto.sync.SyncResult;
import com.lre.gitlabintegration.dto.sync.SyncStateEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SyncAnalyzer {

    public SyncResult analyze(List<SyncStateEntry> previous, List<GitLabCommit> current) {
        List<SyncStateEntry> prevList = previous != null ? previous : List.of();
        List<GitLabCommit> currList = current != null ? current : List.of();

        if (prevList.isEmpty() && currList.isEmpty()) {
            return new SyncResult(List.of(), List.of(), List.of());
        }

        Map<String, SyncStateEntry> prevByPath = indexPrevious(prevList);
        Map<String, GitLabCommit> currByPath = indexCurrent(currList);

        List<SyncStateEntry> toDelete = findDeletes(prevList, currByPath);
        Changes changes = findUploadsAndUnchanged(currList, prevByPath);

        log.info("Analysis complete: upload={}, delete={}, unchanged={}",
                changes.toUpload().size(),
                toDelete.size(),
                changes.unchanged().size()
        );

        return new SyncResult(changes.toUpload(), toDelete, changes.unchanged());
    }


    private Map<String, SyncStateEntry> indexPrevious(List<SyncStateEntry> prevList) {
        Map<String, SyncStateEntry> map = new HashMap<>();
        for (SyncStateEntry entry : prevList) {
            if (entry == null || entry.path() == null) continue;
            map.putIfAbsent(entry.path(), entry);
        }
        return map;
    }

    private Map<String, GitLabCommit> indexCurrent(List<GitLabCommit> currList) {
        Map<String, GitLabCommit> map = new HashMap<>();
        for (GitLabCommit commit : currList) {
            if (commit == null || commit.getPath() == null) continue;
            map.putIfAbsent(commit.getPath(), commit);
        }
        return map;
    }

    private List<SyncStateEntry> findDeletes(List<SyncStateEntry> prevList,
                                             Map<String, GitLabCommit> currByPath) {
        List<SyncStateEntry> toDelete = new ArrayList<>();
        for (SyncStateEntry prev : prevList) {
            if (prev == null || prev.path() == null) continue;
            if (!currByPath.containsKey(prev.path())) {
                toDelete.add(prev);
            }
        }
        return toDelete;
    }

    private Changes findUploadsAndUnchanged(List<GitLabCommit> currList, Map<String, SyncStateEntry> prevByPath) {
        List<GitLabCommit> toUpload = new ArrayList<>();
        List<GitLabCommit> unchanged = new ArrayList<>();

        for (GitLabCommit cur : currList) {
            if (cur == null || cur.getPath() == null) continue;
            SyncStateEntry prev = prevByPath.get(cur.getPath());
            boolean upload = prev == null || hasChanged(prev, cur);
            if (upload) toUpload.add(cur);
            else unchanged.add(cur);

        }

        return new Changes(toUpload, unchanged);
    }

    private boolean hasChanged(SyncStateEntry previous, GitLabCommit current) {
        String prevSha = previous.commitSha();
        String curSha = current.getSha();

        if (prevSha == null || prevSha.isBlank() || curSha == null || curSha.isBlank()) {
            return !Objects.equals(prevSha, curSha);
        }
        return !prevSha.equals(curSha);
    }

    private record Changes(List<GitLabCommit> toUpload, List<GitLabCommit> unchanged) {
    }
}
