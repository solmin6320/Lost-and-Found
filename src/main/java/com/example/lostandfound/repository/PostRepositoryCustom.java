package com.example.lostandfound.repository;

import com.example.lostandfound.dto.request.PostSearchCondition;
import com.example.lostandfound.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// 메서드 이름으로 만들 수 없는 쿼리는 직접 구현
public interface PostRepositoryCustom {

    Page<Post> search(PostSearchCondition condition, Pageable pageable);
}
