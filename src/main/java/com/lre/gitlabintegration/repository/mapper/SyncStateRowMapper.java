package com.lre.gitlabintegration.repository.mapper;

import com.lre.gitlabintegration.dto.sync.SyncStateRow;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SyncStateRowMapper implements RowMapper<SyncStateRow> {

    @Override
    public SyncStateRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        SyncStateRow row = new SyncStateRow();
        row.setSha(rs.getString("commit_sha"));
        row.setPath(rs.getString("script_path_unix"));
        row.setCommittedDate(rs.getString("committed_date"));
        row.setLreScriptId((Integer) rs.getObject("lre_script_id"));
        return row;
    }
}
