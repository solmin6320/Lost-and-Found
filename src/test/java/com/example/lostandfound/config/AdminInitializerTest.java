package com.example.lostandfound.config;

import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Role;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("목록에 있는 가입 계정은 관리자로 올리고 다시 로그인하게 함(대소문자, 공백 무시)")
    void promotesListedMember() {

        Member member = member(1L, "admin@test.com");
        given(memberRepository.findAllByRole(Role.ADMIN)).willReturn(List.of());

        given(memberRepository.findByEmail("admin@test.com")).willReturn(Optional.of(member));

        initializer("  Admin@Test.com").run(null);

        assertThat(member.getRole()).isEqualTo(Role.ADMIN);
        verify(refreshTokenService).delete(1L);
    }

    @Test
    @DisplayName("목록에서 빠진 관리자는 USER로 내림")
    void demotesUnlistedAdmin() {

        Member old = member(2L, "old@test.com");
        old.changeRole(Role.ADMIN);

        given(memberRepository.findAllByRole(Role.ADMIN)).willReturn(List.of(old));

        initializer().run(null);

        assertThat(old.getRole()).isEqualTo(Role.USER);
        verify(refreshTokenService).delete(2L);
    }

    @Test
    @DisplayName("이미 관리자면 그대로 두고, 가입하지 않은 이메일은 건너뜀")
    void keepsExistingAdminAndSkipsUnknown() {

        Member admin = member(3L, "admin@test.com");
        admin.changeRole(Role.ADMIN);

        given(memberRepository.findAllByRole(Role.ADMIN)).willReturn(List.of(admin));

        given(memberRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));

        given(memberRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        initializer("admin@test.com", "ghost@test.com").run(null);


        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        verify(refreshTokenService, never()).delete(anyLong());


    }


    private AdminInitializer initializer(String... emails) {
        return new AdminInitializer(new AdminProperties(List.of(emails)), memberRepository, refreshTokenService);
    }


    private Member member(Long id, String email) {
        Member member = Member.builder()
                .email(email)
                .password("encoded")
                .nickname("회원" + id)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
