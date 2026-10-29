package com.example.lostandfound.client;

import java.time.LocalDate;
import java.util.List;

// 공공 습득물 API 한 쪽
public record PublicFoundPage(

        int totalCount,
        List<Row> rows
) {

    // 습득물 한 건(필요한 칸만)
    public record Row(
            String atcId,
            String fdSn,
            String itemName,
            String categoryName,
            String colorName,
            LocalDate foundDate,
            String storagePlace,
            String imageUrl,
            String title
    ) {

        // 관리ID + 습득순번
        public String externalId() {
            return atcId + "-" + fdSn;
        }
    }
}
