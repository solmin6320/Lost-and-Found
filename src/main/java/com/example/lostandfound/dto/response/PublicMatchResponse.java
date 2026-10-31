package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.PublicFoundItem;
import com.example.lostandfound.entity.PublicFoundSource;

import java.time.LocalDate;

public record PublicMatchResponse(

        Long itemId,
        PublicFoundSource source,
        String itemName,
        String categoryName, // 원문 분류명
        String colorName,
        LocalDate foundDate,
        String storagePlace,
        String imageUrl,
        long daysApart, // 분실일과 습득일 차이
        boolean similarName
) {

    public static PublicMatchResponse of(PublicFoundItem item, long daysApart, boolean similarName) {
        return new PublicMatchResponse(
                item.getId(),
                item.getSource(),
                item.getItemName(),

                item.getCategoryName(),
                item.getColorName(),
                item.getFoundDate(),

                item.getStoragePlace(),
                item.getImageUrl(),
                daysApart,
                similarName
        );
    }
}
