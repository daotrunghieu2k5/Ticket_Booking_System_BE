-- venue table per DATABASE.md §5.2.
-- Columns: id, name, address, capacity, created_at, updated_at.
-- Uniqueness on name is enforced at application level (existsByNameIgnoreCase)
-- rather than DB level — consistent with the category pattern.
CREATE TABLE venue (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    address    VARCHAR(500) NOT NULL,
    capacity   INTEGER      NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
