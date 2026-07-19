CREATE TABLE users (

                        id BIGSERIAL PRIMARY KEY,

                        full_name VARCHAR(100) NOT NULL,

                        email VARCHAR(150) NOT NULL UNIQUE,

                        password VARCHAR(255) NOT NULL,

                        phone VARCHAR(20),

                        avatar VARCHAR(255),

                        status VARCHAR(20) NOT NULL,

                        email_verified BOOLEAN NOT NULL DEFAULT FALSE,

                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email
    ON users(email);

CREATE INDEX idx_user_status
    ON users(status);