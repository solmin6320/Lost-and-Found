package com.example.lostandfound.controller;

import com.example.lostandfound.dto.request.PostCreateRequest;
import com.example.lostandfound.dto.response.PostResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // POST /api/posts
    // 폼 필드를 DTO 생성자로 바인딩
    @PostMapping
    public ResponseEntity<PostResponse> create(
            @Valid @ModelAttribute PostCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        PostResponse response = postService.create(request, userDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 리소스 생성 성공(201)
    }
}
