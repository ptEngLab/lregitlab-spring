package com.lre.gitlabintegration.repository.mapper;

import com.lre.gitlabintegration.dto.gitlab.GitLabCommit;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GitLabCommitRowMapper implements RowMapper<GitLabCommit> {

    @Override
    public GitLabCommit mapRow(ResultSet rs, int rowNum) throws SQLException {
        GitLabCommit commit = new GitLabCommit();
        commit.setSha(rs.getString("commit_sha"));
        commit.setPath(rs.getString("script_path_unix"));
        commit.setCommittedDate(rs.getString("committed_date"));
        return commit;
    }
}
