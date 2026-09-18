package com.example.lostandfound.controller;

import com.example.lostandfound.dto.request.NicknameUpdateRequest;
import com.example.lostandfound.dto.request.PasswordUpdateRequest;
import com.example.lostandfound.dto.response.MemberResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // GET /api/members/me
    // 인증이 보장된 경로이므로 null 체크 불필요
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        return ResponseEntity.ok(memberService.getMyInfo(userDetails.getMemberId()));
    }

    // PATCH /api/members/me
    @PatchMapping("/me")
    public ResponseEntity<MemberResponse> updateNickname(
            @Valid @RequestBody NicknameUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        return ResponseEntity.ok(memberService.updateNickname(userDetails.getMemberId(), request));
    }

    // PATCH /api/members/me/password
    // 돌려줄 본문이 없으므로 204
    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(
            @Valid @RequestBody PasswordUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {

        memberService.updatePassword(userDetails.getMemberId(), request);

        return ResponseEntity.noContent().build();
    }
}
