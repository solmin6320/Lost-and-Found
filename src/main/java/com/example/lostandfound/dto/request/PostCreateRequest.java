package com.example.lostandfound.dto.request;

import com.example.lostandfound.entity.PostCategory;
import com.example.lostandfound.entity.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// 폼 필드를 생성자로 바인딩
public record PostCreateRequest(

        // 문자열이 Enum으로 변환되지 않으면 바인딩 단계에서 실패
        @NotNull(message = "유형은 필수입니다")
        PostType type,

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다")
        String title,

        // TEXT의 상한을 둠
        @NotBlank(message = "본문은 필수입니다")
        @Size(max = 5000, message = "본문은 5000자를 초과할 수 없습니다")
        String content,

        @NotNull(message = "카테고리는 필수입니다")
        PostCategory category,

        @NotBlank(message = "장소는 필수입니다")
        @Size(max = 100, message = "장소는 100자를 초과할 수 없습니다")
        String location,

        // 미래 날짜가 섞이는 것을 방지
        @NotNull(message = "분실ㆍ습득 일자는 필수입니다")
        @PastOrPresent(message = "분실ㆍ습득 일자는 오늘 이후일 수 없습니다")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // 폼 필드는 문자열로 오므로 형식 명시
        LocalDate lostFoundDate
) {

}
