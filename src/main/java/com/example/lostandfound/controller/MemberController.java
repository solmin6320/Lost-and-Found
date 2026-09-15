package com.example.lostandfound.controller;

import com.example.lostandfound.dto.response.MemberResponse;
import com.example.lostandfound.security.CustomUserDetails;
import com.example.lostandfound.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
