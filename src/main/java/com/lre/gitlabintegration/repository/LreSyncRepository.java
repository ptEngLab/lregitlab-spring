package com.lre.gitlabintegration.repository;

import com.lre.gitlabintegration.dto.lreuser.LreUserDto;
import com.lre.gitlabintegration.dto.lreuser.LreUserRoleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LreSyncRepository {

    private final JdbcTemplate jdbc;

    public void upsertUsers(List<LreUserDto> users) {
        jdbc.batchUpdate("""
                        INSERT INTO lre_user (lre_user_id, lre_username, full_name, status, last_update_date, email)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT(lre_user_id) DO UPDATE SET
                          lre_username = excluded.lre_username,
                          full_name = excluded.full_name,
                          status = excluded.status,
                          last_update_date = excluded.last_update_date,
                          email = excluded.email
                        """,
                users,
                200,
                (ps, u) -> {
                    ps.setLong(1, u.id());
                    ps.setString(2, safeTrim(u.userName()));
                    ps.setString(3, safeTrim(u.fullName()));
                    ps.setString(4, safeTrim(u.status()));
                    ps.setString(5, safeTrim(u.lastUpdateDate()));
                    ps.setString(6, safeTrim(u.email()));
                }
        );
    }

    public void replaceAllRoles(List<RoleRow> roles) {
        jdbc.update("DELETE FROM lre_user_role");

        jdbc.batchUpdate("""
                        INSERT OR IGNORE INTO lre_user_role (lre_user_id, domain, project_name, project_id, role)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                roles,
                500,
                (ps, r) -> {
                    ps.setLong(1, r.lreUserId());
                    ps.setString(2, safeTrim(r.domain()));
                    ps.setString(3, safeTrim(r.projectName()));
                    ps.setObject(4, r.projectId()); // Integer or null
                    ps.setString(5, safeTrim(r.role()));
                }
        );
    }

    public static List<RoleRow> flattenRoles(List<LreUserDto> users) {
        List<RoleRow> out = new ArrayList<>();
        for (var user : users) {
            for (var role : rolesOf(user)) {
                out.add(toRoleRow(user, role));
            }
        }
        return out;
    }

    private static List<LreUserRoleDto> rolesOf(LreUserDto user) {
        if (user == null || user.additionalData() == null) return List.of();

        List<LreUserRoleDto> roles = new ArrayList<>();

        for (var ad : user.additionalData()) {
            if (ad == null || ad.userRoles() == null) continue;

            for (var role : ad.userRoles()) {
                if (role != null) roles.add(role);
            }
        }
        return roles;
    }

    private static RoleRow toRoleRow(LreUserDto user, LreUserRoleDto role) {
        return new RoleRow(
                user.id(),
                role.domain(),
                role.projectName(),
                parseProjectId(role.projectId()),
                role.role()
        );
    }

    private static Integer parseProjectId(String projectId) {
        if (projectId == null) return null;

        String s = projectId.trim();
        if (s.isEmpty()) return null;

        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }


    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    public record RoleRow(long lreUserId, String domain, String projectName, Integer projectId, String role) {
    }
}
