package com.example.lostandfound.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PUBLIC_FOUND_ITEM")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicFoundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "public_item_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PublicFoundSource source;

    // 관리ID + 습득순번
    @Column(name = "external_id", nullable = false, length = 30)
    private String externalId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    // 우리 카테고리로 바꾼 값
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostCategory category;

    // 원문 분류명
    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(name = "color_name", length = 100)
    private String colorName;

    @Column(name = "found_date", nullable = false)
    private LocalDate foundDate;

    // 습득 장소가 아니라 보관 장소
    @Column(name = "storage_place", length = 100)
    private String storagePlace;

    // 사진은 주소만 저장
    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Column(length = 300)
    private String title;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Builder
    private PublicFoundItem(PublicFoundSource source, String externalId, String itemName, PostCategory category,
                            String categoryName, String colorName, LocalDate foundDate,
                            String storagePlace, String imageUrl, String title) {

        this.source = source;
        this.externalId = externalId;
        this.itemName = itemName;
        this.category = category;
        this.categoryName = categoryName;
        this.colorName = colorName;
        this.foundDate = foundDate;
        this.storagePlace = storagePlace;
        this.imageUrl = imageUrl;
        this.title = title;
        this.fetchedAt = LocalDateTime.now();
    }

    // 다시 받으면 최신 내용으로 덮어씀
    public void refresh(String itemName, PostCategory category, String categoryName, String colorName,
                        LocalDate foundDate, String storagePlace, String imageUrl, String title) {

        this.itemName = itemName;
        this.category = category;
        this.categoryName = categoryName;
        this.colorName = colorName;
        this.foundDate = foundDate;
        this.storagePlace = storagePlace;
        this.imageUrl = imageUrl;
        this.title = title;
        this.fetchedAt = LocalDateTime.now();
    }
}
