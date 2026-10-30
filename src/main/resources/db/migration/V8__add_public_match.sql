-- 분실 글, 공공 습득물 매칭 후보
CREATE TABLE public_match(
    public_match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lost_post_id BIGINT NOT NULL,
    public_item_id BIGINT NOT NULL,
    score INT NOT NULL,
    dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_public_match_lost_post FOREIGN KEY (lost_post_id) REFERENCES post(post_id) ON DELETE CASCADE,
    CONSTRAINT fk_public_match_item FOREIGN KEY (public_item_id) REFERENCES public_found_item(public_item_id) ON DELETE CASCADE,
    CONSTRAINT uk_public_match_pair UNIQUE (lost_post_id, public_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;