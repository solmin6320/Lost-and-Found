package com.example.lostandfound.controller;

import com.example.lostandfound.dto.request.ClaimRequest;
import com.example.lostandfound.dto.response.ClaimResponse;
import com.example.lostandfound.dto.response.MyClaimResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    // POST /api/posts/{postId}/claims
    @PostMapping("/posts/{postId}/claims")
    public ResponseEntity<ClaimResponse> create(
            @PathVariable Long postId, @Valid @RequestBody ClaimRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        ClaimResponse response = claimService.create(postId, request, userDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201
    }

    // GET /api/posts/{postId}/claims
    @GetMapping("/posts/{postId}/claims")
    public ResponseEntity<List<ClaimResponse>> getReceived(
            @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(claimService.getReceivedClaims(postId, userDetails.getMemberId()));
    }

    // GET /api/members/me/claims?page=&size=
    @GetMapping("/members/me/claims")
    public ResponseEntity<Page<MyClaimResponse>> getMine(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(claimService.getMyClaims(userDetails.getMemberId(), pageable));
    }

    // PATCH /api/claims/{claimId}/approve
    @PatchMapping("/claims/{claimId}/approve")
    public ResponseEntity<ClaimResponse> approve(
            @PathVariable Long claimId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(claimService.approve(claimId, userDetails.getMemberId()));
    }

    // PATCH /api/claims/{claimId}/reject
    @PatchMapping("/claims/{claimId}/reject")
    public ResponseEntity<ClaimResponse> reject(
            @PathVariable Long claimId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(claimService.reject(claimId, userDetails.getMemberId()));
    }

    // PATCH /api/claims/{claimId}/receive
    @PatchMapping("/claims/{claimId}/receive")
    public ResponseEntity<ClaimResponse> receive(
            @PathVariable Long claimId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(claimService.receive(claimId, userDetails.getMemberId()));
    }
}
