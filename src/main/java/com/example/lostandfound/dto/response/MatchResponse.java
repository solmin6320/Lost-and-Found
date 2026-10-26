package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostCategory;
import com.example.lostandfound.entity.PostType;

import java.time.LocalDate;

// 매칭 후보
public record MatchResponse(

        Long postId,
        PostType type,
        String title,
        PostCategory category,
        String location,
        LocalDate lostFoundDate,
        String thumbnailUrl, // 이미지가 없으면 null
        long daysApart, // 분실일과 습득일 차이(0 이상)
        boolean similarPlace,
        boolean similarText
) {

    public static MatchResponse of(Post candidate, String thumbnailUrl, long daysApart, boolean similarPlace, boolean similarText) {
        return new MatchResponse(
                candidate.getId(),
                candidate.getType(),
                candidate.getTitle(),
                candidate.getCategory(),
                candidate.getLocation(),
                candidate.getLostFoundDate(),
                thumbnailUrl,
                daysApart,
                similarPlace,
                similarText
        );
    }
}
