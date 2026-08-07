-- Full event table matching DATABASE.md §5.4.
-- Columns: id, title, description, poster, duration, age_limit, status, category_id,
--           created_at, updated_at.
-- status is stored as VARCHAR per project convention (enums stored as VARCHAR).
-- duration is stored in minutes (INTEGER).
-- age_limit is the minimum age requirement (INTEGER).
CREATE TABLE event (
    id          BIGSERIAL    PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    poster      VARCHAR(500),
    duration    INTEGER      NOT NULL,
    age_limit   INTEGER      NOT NULL DEFAULT 0,
    status      VARCHAR(50)  NOT NULL,
    category_id BIGINT       NOT NULL REFERENCES category(id),
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
