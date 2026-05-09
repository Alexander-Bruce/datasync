-- User 表
CREATE TABLE IF NOT EXISTS User (
                                    id INTEGER,
                                    username TEXT NOT NULL,
                                    email TEXT NOT NULL UNIQUE,
                                    avatar TEXT,
                                    ip TEXT,
                                    user_agent TEXT,
                                    refresh_token TEXT,
                                    access_token TEXT,
                                    PRIMARY KEY(username) -- 假设 username 唯一，如果有其他主键需求可以改
    );

DROP TABLE IF EXISTS File;

CREATE TABLE IF NOT EXISTS File (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    alias TEXT,
                                    path TEXT,
                                    romote_host TEXT,
                                    scheduled TEXT,
                                    cdc_alg TEXT CHECK(cdc_alg IN ('FastCDC','FlipCDC','QuickCDC','RabinCDC')),
    is_dir BOOLEAN DEFAULT 0,
    is_sync BOOLEAN DEFAULT 0,
    description TEXT      -- 新增列
    );


-- SubFile 表
CREATE TABLE IF NOT EXISTS SubFile (
                                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                                       file_id INTEGER NOT NULL,
                                       relative_path TEXT,
                                       depth INTEGER,
                                       is_dir BOOLEAN DEFAULT 0,
                                       is_sync BOOLEAN DEFAULT 0,
                                       FOREIGN KEY(file_id) REFERENCES File(id) ON DELETE CASCADE
    );
