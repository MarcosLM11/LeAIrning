CREATE TABLE IF NOT EXISTS file_metadata (
    id           uuid                        NOT NULL PRIMARY KEY,
    version      bigint                      NOT NULL,
    name         varchar(255)                NOT NULL,
    content_type varchar(255)                NOT NULL,
    size         bigint                      NOT NULL,
    owner_id     uuid,
    status       varchar(255)                NOT NULL CHECK (status IN ('UPLOADING', 'UPLOADED', 'READY', 'FAILED')),
    uploaded_at  timestamp(6) with time zone NOT NULL
);
