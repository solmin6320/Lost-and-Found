package com.example.lostandfound.dto.request;

import com.example.lostandfound.entity.PostCategory;
import com.example.lostandfound.entity.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// 검증 규칙은 등록과 동일
public record PostUpdateRequest(

        @NotNull(message = "유형은 필수입니다")
        PostType postType,

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
        String title,

        @NotBlank(message = "본문은 필수입니다")
        @Size(max = 5000, message = "본문은 5000자를 초과할 수 없습니다")
        String content,

        @NotNull(message = "카테고리는 필수입니다")
        PostCategory postCategory,

        @NotBlank(message = "장소는 필수입니다")
        @Size(max = 100, message = "장소는 100자를 초과할 수 없습니다")
        String location,

        @NotNull(message = "분실ㆍ습득 일자는 필수입니다")
        @PastOrPresent(message = "분실ㆍ습득 일자는 오늘 이후일 수 없습니다")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate lostFoundDate,

        // 기존 이미지를 지우겠다는 명시적 의사
        Boolean removeImages

) {

    // 안 보내면 null로 오므로 false 고정
    public PostUpdateRequest {
        removeImages = (removeImages != null) && removeImages;
    }
}
