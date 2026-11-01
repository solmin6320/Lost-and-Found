package com.example.lostandfound.repository;

import com.example.lostandfound.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {


    // 방마다 마지막 메시지
    @Query("select m from ChatMessage m where m.id in (" +
            "select max(m2.id) from ChatMessage m2 where m2.room.id in :roomIds group by m2.room.id)")
    List<ChatMessage> findLastMessages(@Param("roomIds") List<Long> roomIds);

    // 방마다 안 읽은 수
    @Query("select m.room.id, count(m) from ChatMessage m join m.room r " +
            "where r.id in :roomIds " +
            "and (m.sender is null or m.sender.id <> :memberId) " +
            "and m.id > coalesce(case when r.writer.id = :memberId then r.writerLastReadId else r.requesterLastReadId end, 0) " +
            "group by m.room.id")
    List<Object[]> countUnread(@Param("roomIds") List<Long> roomIds, @Param("memberId") Long memberId);
}
