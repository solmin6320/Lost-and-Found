package com.example.lostandfound.controller;

import com.example.lostandfound.dto.response.PublicMatchResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.PublicMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicMatchController {

    private final PublicMatchService publicMatchService;

    // GET /api/posts/{postId}/public-matches
    @GetMapping("/posts/{postId}/public-matches")
    public ResponseEntity<List<PublicMatchResponse>> getPublicMatches(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        return ResponseEntity.ok(publicMatchService.getPublicMatches(postId, userDetails.getMemberId()));
    }

    // PATCH /api/posts/{postId}/public-matches/{itemId}/dismiss
    @PatchMapping("/posts/{postId}/public-matches/{itemId}/dismiss")
    public ResponseEntity<Void> dismiss(
            @PathVariable Long postId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        publicMatchService.dismiss(postId, itemId, userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
