package com.example.lostandfound.repository;

import com.example.lostandfound.entity.PublicMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicMatchRepository extends JpaRepository<PublicMatch, Long> {

    // 같은 쌍이 이미 있는지 확인
    boolean existsByLostPostIdAndPublicItemId(Long lostPostId, Long publicItemId);
}
