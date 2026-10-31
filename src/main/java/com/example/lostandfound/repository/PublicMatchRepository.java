package com.example.lostandfound.repository;

import com.example.lostandfound.entity.PublicMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PublicMatchRepository extends JpaRepository<PublicMatch, Long> {

    // 같은 쌍이 이미 있는지 확인
    boolean existsByLostPostIdAndPublicItemId(Long lostPostId, Long publicItemId);

    // 숨길 후보
    Optional<PublicMatch> findByLostPostIdAndPublicItemId(Long lostPostId, Long publicItemId);

    // 내 분실 글의 공공 습득물 후보
    @Query("select m from PublicMatch m join fetch m.publicItem " +
    "where m.lostPost.id = :postId and m.dismissed = false " +
    "order by m.score desc, m.id desc")
    List<PublicMatch> findCandidates(@Param("postId") Long postId);
}
