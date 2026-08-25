CREATE TABLE MEMBER (
                        member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        email VARCHAR(100) NOT NULL UNIQUE,
                        password VARCHAR(60) NOT NULL,
                        nickname VARCHAR(20) NOT NULL UNIQUE,
                        created_at DATETIME NOT NULL,
                        updated_at DATETIME NULL
);

CREATE TABLE POST (
                      post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      member_id BIGINT NOT NULL,
                      type VARCHAR(10) NOT NULL,
                      title VARCHAR(100) NOT NULL,
                      content TEXT NOT NULL,
                      category VARCHAR(20) NOT NULL,
                      location VARCHAR(100) NOT NULL,
                      lost_found_date DATE NOT NULL,
                      status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                      view_count INT NOT NULL DEFAULT 0,
                      created_at DATETIME NOT NULL,
                      updated_at DATETIME NULL,
                      CONSTRAINT fk_post_member FOREIGN KEY (member_id) REFERENCES MEMBER(member_id)
);

CREATE TABLE POST_IMAGE (
                            image_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            post_id BIGINT NOT NULL,
                            original_filename VARCHAR(255) NOT NULL,
                            stored_filename VARCHAR(50) NOT NULL UNIQUE,
                            file_path VARCHAR(500) NOT NULL,
                            file_size INT NOT NULL,
                            created_at DATETIME NOT NULL,
                            CONSTRAINT fk_image_post FOREIGN KEY (post_id) REFERENCES POST(post_id)
);

CREATE TABLE COMMENT (
                         comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         post_id BIGINT NOT NULL,
                         member_id BIGINT NOT NULL,
                         content VARCHAR(300) NOT NULL,
                         created_at DATETIME NOT NULL,
                         CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES POST(post_id),
                         CONSTRAINT fk_comment_member FOREIGN KEY (member_id) REFERENCES MEMBER(member_id)
);