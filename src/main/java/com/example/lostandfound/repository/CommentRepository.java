package com.example.lostandfound.repository;

import com.example.lostandfound.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // // 작성자 닉네임 때문에 N+1이 나므로 함께 조회
    @EntityGraph(attributePaths = "member")
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    long countByPostId(Long postId);

    // 게시글 삭제 전에 호출
    @Modifying
    @Query("delete from Comment c where c.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
