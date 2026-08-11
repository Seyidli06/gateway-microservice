CREATE TABLE profiles
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL,
    email      VARCHAR(320)             NOT NULL,
    bio        VARCHAR(500),
    version    BIGINT                   NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_profiles_email UNIQUE (email)
);

CREATE INDEX idx_profiles_created_at
    ON profiles (created_at DESC);

CREATE INDEX idx_profiles_name
    ON profiles (name);