package com.example.lostandfound.service;

import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Notification;
import com.example.lostandfound.entity.NotificationType;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.NotificationRepository;
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
import static org.mockito.BDDMockito.given;

// 알림 읽은 처리 단위 테스트
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("내 알림을 읽으면 읽은 시각이 남고, 다시 읽어도 처음 시각을 유지")
    void markRead_keepsFirstReadTime() {
        Notification notification = notificationOf();

        notificationService.markRead(1L, 10L);

        var firstReadAt = notification.getReadAt();
        notificationService.markRead(1L, 10L);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }

    @Test
    @DisplayName("남의 알림은 없는 알림과 같게 404, 읽음 처리되지 않음")
    void markRead_othersNotification() {
        Notification notification = notificationOf();

        assertThatThrownBy(() -> notificationService.markRead(2L, 10L))
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);

        assertThat(notification.isRead()).isFalse();
    }



    // id 10번 알림, 받는 사람은 1번 회원
    private Notification notificationOf() {
            Member receiver = Member.builder()
                    .email("test@test.com")
                    .password("encoded")
                    .nickname("받는사람")
                    .build();
            ReflectionTestUtils.setField(receiver, "id", 1L);

            Notification notification = Notification.builder()
                    .receiver(receiver)
                    .type(NotificationType.CLAIM_REQUESTED)
                    .postId(5L)
                    .build();

            ReflectionTestUtils.setField(notification, "id", 10L);

            given(notificationRepository.findById(10L))
                    .willReturn(Optional.of(notification));
            return notification;
    }
}
