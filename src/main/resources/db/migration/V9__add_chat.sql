-- 1:1 채팅방, 메시지
CREATE TABLE chat_room (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id BIGINT NULL,
    writer_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    post_title VARCHAR(100) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    blocked_by BIGINT NULL,
    writer_last_read_id BIGINT NULL,
    requester_last_read_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    closed_at DATETIME NULL,

    CONSTRAINT fk_chat_room_claim FOREIGN KEY (claim_id) REFERENCES claim(claim_id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_room_writer FOREIGN KEY (writer_id) REFERENCES member(member_id),
    CONSTRAINT fk_chat_room_requester FOREIGN KEY (requester_id) REFERENCES member(member_id),
    CONSTRAINT fk_chat_room_blocked_by FOREIGN KEY (blocked_by) REFERENCES member(member_id),
    CONSTRAINT uk_chat_room_claim UNIQUE (claim_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_message (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NULL,
    type VARCHAR(10) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    masked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,

    CONSTRAINT fk_chat_message_room FOREIGN KEY (room_id) REFERENCES chat_room(room_id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES member(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;