package com.example.lostandfound.repository;

import com.example.lostandfound.entity.Claim;
import com.example.lostandfound.entity.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    // 한 글에 한 사람 한 번
    boolean existsByPostIdAndClaimantId(Long postId, Long claimantId);

    // 받은 요청 목록(글쓴이)
    @Query("select c from Claim c " + "join fetch c.claimant " +
            "where c.post.id = :postId " +
    "order by c.createdAt desc, c.id desc")
    List<Claim> findAllByPostIdWithClaimant(@Param("postId") Long postId);

    // 내가 보낸 요청
    @Query(value = "select c from Claim c " +
    "join fetch c.post " + "where c.claimant.id = :claimantId " +
    "order by c.createdAt desc, c.id desc", countQuery = "select count(c) from Claim c where c.claimant.id = :claimantId")
    Page<Claim> findMyClaims(@Param("claimantId") Long claimantId, Pageable pageable);

    // 상세 - 내가 보낸 요청
    Optional<Claim> findByPostIdAndClaimantId(Long postId, Long claimantId);

    // 상세 - 글쓴이가 볼 대기 중 요청 수
    long countByPostIdAndStatus(Long postId, ClaimStatus status);

    // 승인된 요청이 있는지 확인
    boolean existsByPostIdAndStatus(Long postId, ClaimStatus status);

    // 되찾기 기록
    Optional<Claim> findFirstByPostIdAndStatus(Long postId, ClaimStatus status);

    // 글 삭제 전에 요청 먼저 지우기
    @Modifying
    @Query("delete from Claim c where c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
