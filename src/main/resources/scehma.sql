
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
