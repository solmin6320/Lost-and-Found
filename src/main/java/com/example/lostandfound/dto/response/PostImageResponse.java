package com.example.lostandfound.dto.response;

import com.example.lostandfound.entity.PostImage;

// 첨부 이미지 한 장
public record PostImageResponse(

        Long id,
        String originalFilename,
        String filePath
) {

    public static PostImageResponse from(PostImage image) {

        return new PostImageResponse(
                image.getId(),
                image.getOriginalFilename(),
                image.getFilePath()
        );
    }
}
