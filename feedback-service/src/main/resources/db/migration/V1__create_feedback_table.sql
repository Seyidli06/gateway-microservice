CREATE TABLE feedback (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          email VARCHAR(320) NOT NULL,
                          message VARCHAR(2000) NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_feedback_created_at
    ON feedback (created_at DESC);

CREATE INDEX idx_feedback_email
    ON feedback (email);