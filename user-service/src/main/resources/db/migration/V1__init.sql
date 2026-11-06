CREATE TABLE IF NOT EXISTS users (
    id                  uuid                     NOT NULL PRIMARY KEY,
    version             bigint                   NOT NULL,
    email               varchar(255)             NOT NULL UNIQUE,
    username            varchar(255)             NOT NULL UNIQUE,
    password            varchar(255)             NOT NULL,
    name                varchar(255)             NOT NULL,
    avatar_content_type varchar(50),
    role                varchar(255)             NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at          timestamp(6) with time zone NOT NULL
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         uuid                     NOT NULL PRIMARY KEY,
    version    bigint                   NOT NULL,
    token_hash varchar(255)             NOT NULL UNIQUE,
    user_id    uuid                     NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL
);
