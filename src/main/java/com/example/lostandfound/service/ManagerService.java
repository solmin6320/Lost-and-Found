package com.example.lostandfound.service;

import com.example.lostandfound.dto.response.AdminMemberResponse;
import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Role;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 매니저 임명, 해임(관리자 전용)
@Service
@RequiredArgsConstructor
public class ManagerService {

    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;

    // 매니저 임명
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void appoint(Long memberId) {
        Member member = findChangeableMember(memberId);

        if (member.getRole() == Role.MANAGER) {
            throw new CustomException(ErrorCode.ALREADY_MANAGER);
        }

        changeRole(member, Role.MANAGER);
    }

    // 매니저 해임(다시 USER)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void dismiss(Long memberId) {
        Member member = findChangeableMember(memberId);

        if (member.getRole() != Role.MANAGER) {
            throw new CustomException(ErrorCode.NOT_MANAGER);
        }
        changeRole(member, Role.USER);
    }

    // 회원 검색
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<AdminMemberResponse> searchMembers(String keyword, Role role, Pageable pageable) {

        String trimmed = (keyword == null || keyword.isBlank()) ? null : keyword.strip();

        return memberRepository.searchForAdmin(trimmed, role, pageable)
                .map(AdminMemberResponse::from);
    }

    private Member findChangeableMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getRole() == Role.ADMIN) {
            throw new CustomException(ErrorCode.ADMIN_ROLE_LOCKED);
        }

        return member;
    }

    // 역할 변경 후 리프레시 토큰 삭제(다시 로그인)
    private void changeRole(Member member, Role role) {
        member.changeRole(role);
        refreshTokenService.delete(member.getId());
    }
}
