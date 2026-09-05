package com.example.lostandfound.repository;

import com.example.lostandfound.entity.Post;
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
}
