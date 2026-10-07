-- 주인 확인 질문
ALTER TABLE post
    ADD COLUMN verification_question VARCHAR(200) NULL,
    ADD COLUMN owner_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 주인 확인 요청
CREATE TABLE claim (
    claim_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    claimant_id BIGINT NOT NULL,
    question VARCHAR(200) NULL,
    answer VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    decided_at DATETIME NULL,
    received_at DATETIME NULL,

    CONSTRAINT fk_claim_post FOREIGN KEY (post_id) REFERENCES post(post_id),
    CONSTRAINT fk_claim_member FOREIGN KEY (claimant_id) REFERENCES member(member_id),
    CONSTRAINT uk_claim_post_claimant UNIQUE (post_id, claimant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;