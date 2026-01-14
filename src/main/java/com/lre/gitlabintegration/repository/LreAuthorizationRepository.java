package com.lre.gitlabintegration.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LreAuthorizationRepository {

    private final JdbcTemplate jdbc;

    public boolean isUserActiveAndHasAccess(String username, String domain, String project) {

        Integer exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM lre_user u
                    WHERE u.lre_username = ?
                      AND u.status = 'active'
                      AND EXISTS (
                          SELECT 1
                          FROM lre_user_role r
                          WHERE r.lre_user_id = u.lre_user_id
                            AND r.domain = ?
                            AND r.project_name = ?
                      )
                )
                """,
                Integer.class,
                username, domain, project
        );

        return exists != null && exists == 1;
    }
}
