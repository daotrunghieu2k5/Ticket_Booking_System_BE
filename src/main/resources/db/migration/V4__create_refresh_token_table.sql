CREATE TABLE refresh_token (

                               id BIGSERIAL PRIMARY KEY,

                               token VARCHAR(255) NOT NULL UNIQUE,

                               expired_at TIMESTAMP NOT NULL,

                               revoked BOOLEAN NOT NULL DEFAULT FALSE,

                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               user_id BIGINT NOT NULL,

                               CONSTRAINT fk_refresh_token_user
                                   FOREIGN KEY(user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE --Nếu một người dùng bị xóa khỏi bảng users,
                                                        -- toàn bộ các dòng refresh token liên quan đến người dùng đó
                                                        --trong bảng này cũng sẽ tự động bị xóa sạch theo
);
-- Khi người dùng truy cập hoặc đăng xuất, hệ thống sẽ liên tục chạy câu lệnh:
-- SELECT * FROM refresh_token WHERE user_id = ....
-- Có index này sẽ giúp PostgreSQL tìm kiếm token của user đó ngay lập tức,
-- thay vì phải quét qua hàng triệu dòng từ đầu đến cuối bảng, giúp hệ thống chạy nhanh gấp nhiều lần.
CREATE INDEX idx_refresh_token_user
    ON refresh_token(user_id);