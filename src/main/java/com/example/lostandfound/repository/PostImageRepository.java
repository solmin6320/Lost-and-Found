package com.example.lostandfound.repository;

import com.example.lostandfound.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    // 게시글마다 먼저 올린 이미지 1장
    @Query(value = """
Select pi.post_id as postId, pi.file_path as filePath
From post_image pi
where pi.image_id IN (
Selct MIN(image_id) from post_image
where post_id IN (:postIds)
Group By post_id
)
""", nativeQuery = true) // 네이티브 쿼리 사용
    List<ThumbnailView> findThumbnails(@Param("postIds")Collection<Long> postIds);

    interface ThumbnailView {
        Long getPostId();
        String getFilePath();
    }
}
