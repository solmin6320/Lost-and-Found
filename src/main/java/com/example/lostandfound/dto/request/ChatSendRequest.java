package com.example.lostandfound.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 채팅 메시지 보내기
public record ChatSendRequest(

        @NotBlank(message = "메시지 내용은 필수 입니다")
        @Size(max = 1000, message = "메시지는 1000자를 초과할 수 없습니다")
        String content
) {
}
