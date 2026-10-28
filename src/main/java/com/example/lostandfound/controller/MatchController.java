package com.example.lostandfound.controller;

import com.example.lostandfound.dto.response.MatchResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    // GET /api/posts/{postId}/matches
    @GetMapping("/posts/{postId}/matches")
    public ResponseEntity<List<MatchResponse>> getMatches(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        return ResponseEntity.ok(matchService.getMatches(postId, userDetails.getMemberId()));
    }

    // PATCH /api/posts/{postId}/matches/{candidatePostId}/dismiss
    @PatchMapping("/posts/{postId}/matches/{candidatePostId}/dismiss")
    public ResponseEntity<Void> dismiss(
            @PathVariable Long postId,
            @PathVariable Long candidatePostId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        matchService.dismiss(postId, candidatePostId, userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
