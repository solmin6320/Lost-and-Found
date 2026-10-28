package com.example.lostandfound.repository;

import com.example.lostandfound.entity.PostMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostMatchRepository extends JpaRepository<PostMatch, Long> {

    // 같은 쌍이 이미 있는지 확인
    Optional<PostMatch> findByLostPostIdAndFoundPostId(Long lostPostId, Long foundPostId);

    // 내 분실 글의 습득 후보
    @Query("select m from PostMatch m join fetch m.foundPost " +
    "where m.lostPost.id = :postId and m.dismissedByLost = false " +
    "order by m.score desc, m.id desc")
    List<PostMatch> findCandidatesForLost(@Param("postId") Long postId);

    // 내 습득 글의 분실 후보
    @Query("select m from PostMatch m join fetch m.lostPost " +
    "where m.foundPost.id = :postId and m.dismissedByFound = false " +
    "order by m.score desc, m.id desc")
    List<PostMatch> findCandidatesForFound(@Param("postId") Long postId);
}
