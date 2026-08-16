package com.example.lostandfound.security;

import com.example.lostandfound.entity.Member;
import com.example.lostandfound.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;


    // 조회 전용이므로 readOnly
    // 영속성 컨텍스트의 변경 감지 비용을 줄임
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // 커스텀 예외가 아닌 Security 표준 예외를 던짐
        // 커스텀 예외를 던지면 Security 인증 흐름이 처리 못 함
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다"));

        // Member 엔티티를 UserDetails로 감싸서 반환
        return new CustomUserDetails(member);


    }

}
