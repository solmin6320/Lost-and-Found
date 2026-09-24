package com.example.lostandfound.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 등록, 수정 공용
public record CommentRequest(

        @NotBlank(message = "댓글 내용은 필수입니다")
        @Size(max = 300, message = "댓글은 300자를 초과할 수 없습니다")
        String content
) {
}
