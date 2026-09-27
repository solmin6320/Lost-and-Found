package com.example.lostandfound.repository;

import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// JPQL 사용(Method Query 사용 불가)
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    // 작성자와 이미지를 한 번에
    @Query("select p from Post p " +
    "join fetch p.member " +
    "left join fetch p.images " +
    "where p.id = :id")
    Optional<Post> findDetailById(@Param("id") Long id);

    // DB가 읽고 더하므로 동시 요청에도 유실되지 않음
    @Modifying // 조회가 아닌 변경 쿼리
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void increaseViewCount(@Param("id") Long id);

    // 마이페이지
    // 본인 글, status가 null이면 전체
    @Query("select p from Post p " +
    "where p.member.id = :memberId " +
    "and (:status is null or p.status = :status) " + "order by p.createdAt desc, p.id desc")
    Page<Post> findMyPosts(@Param("memberId") Long memberId,
                           @Param("status")PostStatus status,
                           Pageable pageable);
}
