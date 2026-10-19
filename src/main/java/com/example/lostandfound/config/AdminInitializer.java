package com.example.lostandfound.config;

// 앱이 뜰 때 관리자 목록

import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Role;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        Set<String> adminEmails = new HashSet<>(adminProperties.emails());

        // 목록에서 빠진 관리자는 USER로
        for (Member member : memberRepository.findAllByRole(Role.ADMIN)) {
            if (!adminEmails.contains(member.getEmail())) {
                changeRole(member, Role.USER);
            }
        }

        // 목록에 있는 가입 계정은 ADMIN
        int notJoined = 0;
        for (String email : adminEmails) {
            Member member = memberRepository.findByEmail(email)
                    .orElse(null);

            if (member == null) {
                notJoined++;
            } else if (member.getRole() != Role.ADMIN) {
                changeRole(member, Role.ADMIN);
            }
        }

        // 이메일은 로그에 남기지 않음
        if (notJoined > 0) {
            log.warn("관리자 목록 중 가입하지 않은 이메일 {}개", notJoined);
        }
    }

    // 역할이 바뀌면 다시 로그인하도록 리프레시 토큰 삭제
    private void changeRole(Member member, Role role) {
        log.info("관리자 목록에 따라 역할 변경: memberId={}, {} -> {}", member.getId(), member.getRole(), role);
        member.changeRole(role);
        refreshTokenService.delete(member.getId());
    }

}
