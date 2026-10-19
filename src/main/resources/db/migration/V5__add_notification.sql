-- 알림
CREATE TABLE notification (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    post_id BIGINT NULL,
    claim_id BIGINT NULL,
    post_title VARCHAR(100) NULL,
    actor_nickname VARCHAR(20) NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_notification_member FOREIGN KEY (receiver_id) REFERENCES member(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;