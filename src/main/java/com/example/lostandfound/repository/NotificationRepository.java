package com.example.lostandfound.repository;

import com.example.lostandfound.entity.Notification;
import com.example.lostandfound.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 내 알림(최신 순)
    Page<Notification> findByReceiverIdOrderByIdDesc(Long receiverId, Pageable pageable);

    // 안 읽은 알림 수
    long countByReceiverIdAndReadAtIsNull(Long receiverId);

    // 재연결 때 놓친 알림(최대 50개)
    List<Notification> findTop50ByReceiverIdAndIdGreaterThanOrderByIdAsc(Long receiverId, Long lastId);

    // 오늘 받은 매칭 알림 수
    long countByReceiverIdAndTypeAndCreatedAtAfter(Long receiverId, NotificationType type, LocalDateTime after);

    // 오늘 받은 매칭 알림 수(일반 + 공공 합계)
    long countByReceiverIdAndTypeInAndCreatedAtAfter(Long receiverId, List<NotificationType> types, LocalDateTime after);

    // 모두 읽음
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readAt = :now " +
    "where n.receiver.id = :receiverId and n.readAt is null")
    int markAllRead(@Param("receiverId") Long receiverId, @Param("now")LocalDateTime now);
}
