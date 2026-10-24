-- 분실 글, 습득 글 매칭 후보
CREATE TABLE post_match (
    match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lost_post_id BIGINT NOT NULL,
    found_post_id BIGINT NOT NULL,
    score INT NOT NULL,
    dismissed_by_lost BOOLEAN NOT NULL DEFAULT FALSE,
    dismissed_by_found BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_match_lost_post FOREIGN KEY (lost_post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    CONSTRAINT fk_match_found_post FOREIGN KEY (found_post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    CONSTRAINT uk_match_pair UNIQUE (lost_post_id, found_post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;