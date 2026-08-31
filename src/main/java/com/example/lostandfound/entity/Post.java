package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "POST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    // 작성자
    @ManyToOne(fetch = FetchType.LAZY) // EAGER 타입이면 N+1 문제 발생
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // LOST, FOUND
    @Enumerated(EnumType.STRING) // ORDINAL 타입이면 순서 변경 시  기존 데이터가 뒤집힘
    @Column(nullable = false, length = 10)
    private PostType type;

    // 제목
    @Column(nullable = false, length = 100)
    private String title;

    // 본문
    // columnDefinition을 명시하지 않으면 varchar(255)로 간주
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 카테고리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostCategory category;

    // 자유 입력 장소
    @Column(nullable = false, length = 100)
    private String location;

    // 분실, 습득 일자(날짜만)
    @Column(name = "lost_found_date", nullable = false)
    private LocalDate lostFoundDate;

    // 게시물 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    // 게시글 조회수
    @Column(name = "view_count", nullable = false)
    private int viewCount;

    // 생성일
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 수정일
    private LocalDateTime updatedAt;


    // 첨부 이미지(최대 5장)
    // cascade를 사용하여 게시글 저장, 삭제가 이미지에도 전파
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> images = new ArrayList<>();



    // Builder로만 생성 가능하도록 강제
    @Builder
    private Post(Member member, PostType type, String title, String content,
                 PostCategory category, String location, LocalDate lostFoundDate) {
        this.member = member;
        this.type = type;
        this.title = title;
        this.content = content;
        this.category = category;
        this.location = location;
        this.lostFoundDate = lostFoundDate;

        this.status = PostStatus.OPEN;   // 등록 시 항상 게시중
        this.viewCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    // 양방향 연관관계 편의 메서드
    public void addImage(PostImage image) {
        this.images.add(image);
        image.assignPost(this);
    }



}
