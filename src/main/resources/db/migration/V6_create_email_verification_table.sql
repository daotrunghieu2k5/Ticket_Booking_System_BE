CREATE TABLE email_verification (
                                    id BIGSERIAL PRIMARY KEY,

                                    full_name VARCHAR(255) NOT NULL,

                                    email VARCHAR(255) NOT NULL UNIQUE,

                                    password VARCHAR(255) NOT NULL,

                                    phone VARCHAR(20),

                                    otp_code VARCHAR(6) NOT NULL,

                                    expired_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,

                                    attempt_count SMALLINT NOT NULL DEFAULT 0,

                                    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);