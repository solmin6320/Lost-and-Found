-- 경찰서, 기관에 보관된 습득물(공공데이터)
CREATE TABLE public_found_item (
public_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
source VARCHAR(10) NOT NULL,
external_id VARCHAR(30) NOT NULL,
item_name VARCHAR(200) NOT NULL,
category VARCHAR(20) NOT NULL,
category_name VARCHAR(100),
color_name VARCHAR(100),
found_date DATE NOT NULL,
storage_place VARCHAR(100),
image_url VARCHAR(300),
title VARCHAR(300),
fetched_at DATETIME NOT NULL,

CONSTRAINT uk_public_item_source_external UNIQUE (source, external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;