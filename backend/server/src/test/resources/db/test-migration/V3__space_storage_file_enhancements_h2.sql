-- 空间新增存储方案和默认可见性
ALTER TABLE space ADD COLUMN storageType VARCHAR(32) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE space ADD COLUMN defaultVisibility VARCHAR(32) NOT NULL DEFAULT 'PRIVATE';

-- 文件新增说明、有效期、存储类型
ALTER TABLE fileRecord ADD COLUMN description VARCHAR(65535);
ALTER TABLE fileRecord ADD COLUMN expiresAt TIMESTAMP;
ALTER TABLE fileRecord ADD COLUMN storageType VARCHAR(32);

-- 文件访问日志表
CREATE TABLE IF NOT EXISTS fileAccessLog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fileId BIGINT NOT NULL,
    spaceId BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    actorType VARCHAR(16) NOT NULL,
    actorId BIGINT,
    actorName VARCHAR(128),
    ip VARCHAR(64),
    userAgent VARCHAR(512),
    details VARCHAR(1024),
    createdAt TIMESTAMP NOT NULL
);

CREATE INDEX idx_fileAccessLog_fileId ON fileAccessLog(fileId);
CREATE INDEX idx_fileAccessLog_spaceId ON fileAccessLog(spaceId);
CREATE INDEX idx_fileAccessLog_createdAt ON fileAccessLog(createdAt);
