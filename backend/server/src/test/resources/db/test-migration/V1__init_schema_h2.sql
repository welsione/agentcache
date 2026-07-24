CREATE TABLE IF NOT EXISTS "user" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) NOT NULL UNIQUE,
    passwordHash VARCHAR(256) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    createdAt TIMESTAMP NOT NULL,
    updatedAt TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS space (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    ownerId BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    createdAt TIMESTAMP NOT NULL,
    updatedAt TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS spaceMember (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spaceId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    createdAt TIMESTAMP NOT NULL,
    CONSTRAINT uk_space_member UNIQUE (spaceId, userId)
);

CREATE TABLE IF NOT EXISTS fileRecord (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spaceId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    originalName VARCHAR(256) NOT NULL,
    contentType VARCHAR(128),
    size BIGINT NOT NULL,
    storagePath VARCHAR(512) NOT NULL,
    checksum VARCHAR(128),
    version INT NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    createdBy BIGINT,
    createdAt TIMESTAMP NOT NULL,
    updatedAt TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS apiKey (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spaceId BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    keyHash VARCHAR(256) NOT NULL,
    keyPrefix VARCHAR(16) NOT NULL,
    role VARCHAR(32) NOT NULL,
    expiresAt TIMESTAMP,
    lastUsedAt TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    createdBy BIGINT NOT NULL,
    createdAt TIMESTAMP NOT NULL
);

INSERT INTO "user" (username, email, passwordHash, role, status, createdAt, updatedAt)
VALUES ('admin', 'admin@agentcache.local',
        '$2b$10$MM9o9elg7NSbsP3Xf.n2kuVfir5GChLRZIRTxsaEsH6KqeF2fDMIq',
        'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);