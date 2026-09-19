package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.PostImage;

// 첨부 이미지 한 장
public record PostImageResponse(

        Long id,
        String originalFilename,
        String url
) {

    // DB에는 객체 키만 있으므로 주소를 붙여 내림
    public static PostImageResponse from(PostImage image, String baseUrl) {

        return new PostImageResponse(
                image.getId(),
                image.getOriginalFilename(),
                baseUrl + "/" + image.getFilePath()
        );
    }
}
