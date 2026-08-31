package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "POST_IMAGE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    // 연관관계 주인(FK 컬럼을 가진 쪽)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 사용자가 올린 원본 파일명
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    // UUID + 확장자
    @Column(name = "stored_filename", nullable = false, unique = true, length = 50)
    private String storedFilename;

    // S3 객체 URL
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    // 파일 사이즈
    @Column(name = "file_size", nullable = false)
    private int fileSize;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private PostImage(String originalFilename, String storedFilename,
                      String filePath, int fileSize) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.createdAt = LocalDateTime.now();
    }

    // 같은 패키지 에서만 호출 가능
    void assignPost(Post post) {
        this.post = post;
    }

}
