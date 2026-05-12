CREATE TABLE IF NOT EXISTS User (
    id INTEGER,
    username TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    avatar TEXT,
    ip TEXT,
    user_agent TEXT,
    refresh_token TEXT,
    access_token TEXT,
    PRIMARY KEY(username)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email_unique ON User(email);

CREATE TABLE IF NOT EXISTS File (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    alias TEXT,
    path TEXT,
    remote_host TEXT,
    scheduled TEXT,
    cdc_alg TEXT CHECK(cdc_alg IN ('FastCDC','FlipCDC','QuickCDC','RabinCDC')),
    is_dir BOOLEAN DEFAULT 0,
    is_sync BOOLEAN DEFAULT 0,
    description TEXT,
    update_time TEXT,
    user_id INTEGER
);

CREATE INDEX IF NOT EXISTS idx_file_user_id ON File(user_id);

CREATE TABLE IF NOT EXISTS SubFile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL,
    parent INTEGER,
    name TEXT,
    relative_path TEXT,
    depth INTEGER,
    is_dir BOOLEAN DEFAULT 0,
    is_sync BOOLEAN DEFAULT 0,
    FOREIGN KEY(file_id) REFERENCES File(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_subfile_file_id ON SubFile(file_id);
