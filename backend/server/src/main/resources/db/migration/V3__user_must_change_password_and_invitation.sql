ALTER TABLE user ADD COLUMN mustChangePassword TINYINT(1) NOT NULL DEFAULT 0;
UPDATE user SET mustChangePassword = 1 WHERE username = 'admin';

CREATE TABLE IF NOT EXISTS invitationToken (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    createdBy BIGINT NOT NULL,
    usedAt DATETIME,
    expiresAt DATETIME NOT NULL,
    createdAt DATETIME NOT NULL
);
