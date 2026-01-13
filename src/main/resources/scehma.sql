CREATE TABLE IF NOT EXISTS allowed_lre_access (
  gitlab_project_id INTEGER NOT NULL,
  gitlab_user_id    INTEGER NOT NULL,
  lre_domain        TEXT NOT NULL,
  lre_project       TEXT NOT NULL,
  enabled           INTEGER NOT NULL DEFAULT 1,
  PRIMARY KEY (gitlab_project_id, gitlab_user_id, lre_domain, lre_project)
);

CREATE INDEX IF NOT EXISTS idx_allowed_lre_access_project
  ON allowed_lre_access(gitlab_project_id);

CREATE INDEX IF NOT EXISTS idx_allowed_lre_access_user
  ON allowed_lre_access(gitlab_user_id);
