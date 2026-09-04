package com.example.lostandfound.dto.request;

import com.example.lostandfound.entity.PostCategory;
import com.example.lostandfound.entity.PostStatus;
import com.example.lostandfound.entity.PostType;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record PostSearchCondition(

        String keyword, // title 또는 content 부분 일치
        PostType type,
        PostCategory category,
        PostStatus status,
        String location, // 부분 일치(자유 입력)

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to
) {

    // 조건 자체가 성립하지 않는 요청
    // 400 예외
    @AssertTrue(message = "시작일이 종료일보다 늦을 수 없습니다")
    public boolean isValidPeriod() {
        return from == null || to == null || !from.isAfter(to);
    }
}
