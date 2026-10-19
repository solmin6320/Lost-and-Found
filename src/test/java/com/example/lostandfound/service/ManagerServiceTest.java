package com.example.lostandfound.service;

import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Role;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 매니저 임명, 해임 단위 테스트
@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private ManagerService managerService;


    @Test
    @DisplayName("일반 회원을 임명하면 매니저가 되고 리프레시 토큰이 지워짐")
    void appoint_success() {

        Member member = memberWith(Role.USER);

        managerService.appoint(1L);

        assertThat(member.getRole()).isEqualTo(Role.MANAGER);

        verify(refreshTokenService).delete(1L);
    }

    @Test
    @DisplayName("이미 매니저면 임명할 수 없음")
    void appoint_alreadyManager() {

        memberWith(Role.MANAGER);

        assertThatThrownBy(() -> managerService.appoint(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALREADY_MANAGER);

        verify(refreshTokenService, never()).delete(anyLong());
    }

    @Test
    @DisplayName("매니저를 해임하면 일반 회원이 되고 리프레시 토큰이 지워짐")
    void dismiss_success() {
        Member member = memberWith(Role.MANAGER);

        managerService.dismiss(1L);

        assertThat(member.getRole()).isEqualTo(Role.USER);

        verify(refreshTokenService).delete(1L);
    }

    @Test
    @DisplayName("매니저가 아니면 해임할 수 없음")
    void dismiss_notManager() {
        memberWith(Role.USER);

        assertThatThrownBy(() -> managerService.dismiss(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_MANAGER);
    }

    @Test
    @DisplayName("관리자는 임명, 해임 모두 대상이 될 수 없음")
    void admin_locked() {

        Member member = memberWith(Role.ADMIN);

        assertThatThrownBy(() -> managerService.appoint(1L))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADMIN_ROLE_LOCKED);

        assertThat(member.getRole()).isEqualTo(Role.ADMIN);

        verify(refreshTokenService, never()).delete(anyLong());
    }

    @Test
    @DisplayName("없는 회원이면 404")
    void memberNotFound() {

        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> managerService.appoint(99L))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }


    private Member memberWith(Role role) {

        Member member = Member.builder()
                .email("target@test.com")
                .password("encoded")
                .nickname("target")
                .build();
        member.changeRole(role);

        ReflectionTestUtils.setField(member, "id", 1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        return member;
    }
}
