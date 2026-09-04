package com.example.lostandfound.controller;

import com.example.lostandfound.dto.request.PostCreateRequest;
import com.example.lostandfound.dto.request.PostSearchCondition;
import com.example.lostandfound.dto.response.PostListResponse;
import com.example.lostandfound.dto.response.PostResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    // GET /api/posts? -> 쿼리 스트링
//    keyword=&type=&category=&location=&from=&to=&status=&page=&size=
    @GetMapping
    public ResponseEntity<Page<PostListResponse>> list(
            @Valid @ModelAttribute PostSearchCondition condition, Pageable pageable
            ) {

        return ResponseEntity.ok(postService.search(condition, pageable));
    }
}
