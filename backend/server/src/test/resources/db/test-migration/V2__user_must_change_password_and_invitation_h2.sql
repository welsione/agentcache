-- H2 版本: 对齐主迁移 V3,补充 user.mustChangePassword 字段与 invitationToken 表。

ALTER TABLE "user" ADD COLUMN mustChangePassword BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE "user" SET mustChangePassword = TRUE WHERE username = 'admin';

CREATE TABLE IF NOT EXISTS invitationToken (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    createdBy BIGINT NOT NULL,
    usedAt TIMESTAMP,
    expiresAt TIMESTAMP NOT NULL,
    createdAt TIMESTAMP NOT NULL
);
