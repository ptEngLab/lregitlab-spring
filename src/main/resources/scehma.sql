
PRAGMA foreign_keys = ON;

-- LRE users
CREATE TABLE lre_user (
  lre_user_id        INTEGER PRIMARY KEY,
  lre_username       TEXT NOT NULL COLLATE NOCASE,
  full_name          TEXT,
  status             TEXT NOT NULL COLLATE NOCASE,
  last_update_date   TEXT,
  email              TEXT
);

-- Case-insensitive uniqueness
CREATE UNIQUE INDEX idx_lre_user_username
  ON lre_user(lre_username);


-- LRE roles
CREATE TABLE lre_user_role (
  lre_user_id   INTEGER NOT NULL,
  domain        TEXT NOT NULL COLLATE NOCASE,
  project_name  TEXT NOT NULL COLLATE NOCASE,
  project_id    INTEGER,
  role          TEXT NOT NULL COLLATE NOCASE,
  PRIMARY KEY (lre_user_id, domain, project_name, role),
  FOREIGN KEY (lre_user_id)
    REFERENCES lre_user(lre_user_id)
    ON DELETE CASCADE
);

-- Supports lookups by domain + project + user
CREATE INDEX idx_lre_role_lookup
  ON lre_user_role(domain, project_name, lre_user_id);


CREATE TABLE IF NOT EXISTS gitlab_project (
  gitlab_project_id     INTEGER PRIMARY KEY,
  name                  TEXT,
  path_with_namespace   TEXT,
  web_url               TEXT,
  last_updated_utc      TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))
);

CREATE INDEX IF NOT EXISTS idx_gitlab_project_path
  ON gitlab_project(path_with_namespace);



CREATE TABLE IF NOT EXISTS audit_gitlab_lre_usage (
  id                INTEGER PRIMARY KEY AUTOINCREMENT,
  ts_utc            TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now')),

  gitlab_project_id INTEGER NOT NULL,
  gitlab_project_name TEXT,          -- optional if you can resolve it
  gitlab_project_path TEXT,          -- e.g. group/subgroup/repo (best for reporting)

  gitlab_user_id    INTEGER NOT NULL,
  gitlab_username   TEXT NOT NULL COLLATE NOCASE,

  lre_domain        TEXT NOT NULL COLLATE NOCASE,
  lre_project       TEXT NOT NULL COLLATE NOCASE,

  ref               TEXT,
  tag               INTEGER NOT NULL DEFAULT 0,

  outcome           TEXT NOT NULL,    -- SUCCESS / FAIL / DENIED
  message           TEXT              -- error or short detail
);

CREATE INDEX IF NOT EXISTS idx_audit_ts ON audit_gitlab_lre_usage(ts_utc);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_gitlab_lre_usage(gitlab_username);
CREATE INDEX IF NOT EXISTS idx_audit_project ON audit_gitlab_lre_usage(gitlab_project_id);
CREATE INDEX IF NOT EXISTS idx_audit_lre ON audit_gitlab_lre_usage(lre_domain, lre_project);
