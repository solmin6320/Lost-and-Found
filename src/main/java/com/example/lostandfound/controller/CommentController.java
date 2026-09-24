package com.example.lostandfound.controller;

import com.example.lostandfound.dto.request.CommentRequest;
import com.example.lostandfound.dto.response.CommentResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // POST /api/posts/{postId}/comments
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        CommentResponse response = commentService.create(postId, request, userDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201
    }
    // GET /api/posts/{postId}/comments?page=
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<CommentResponse>> list(
            @PathVariable Long postId, Pageable pageable
    ) {
        return ResponseEntity.ok(commentService.getComments(postId, pageable.getPageNumber()));
    }

    // PUT /api/comments/{commentId}
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                commentService.update(commentId, request, userDetails.getMemberId()));
    }

    // DELETE /api/comments/{commentId}
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        commentService.delete(commentId, userDetails.getMemberId());

        return ResponseEntity.noContent().build(); // 204
    }

}
