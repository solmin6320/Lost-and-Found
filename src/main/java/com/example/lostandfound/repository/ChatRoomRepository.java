package com.example.lostandfound.repository;

import com.example.lostandfound.entity.ChatRoom;
import com.example.lostandfound.entity.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 요청으로 만든 방
    Optional<ChatRoom> findByClaimId(Long claimId);

    // 두 사람 사이에 이 상태의 방이 있는지
    @Query("select count(r) > 0 from ChatRoom r " +
            "where r.status = :status " +
            "and ((r.writer.id = :memberA and r.requester.id = :memberB) " +
            "or (r.writer.id = :memberB and r.requester.id = :memberA))")
    boolean existsBetween(@Param("memberA") Long memberA, @Param("memberB") Long memberB, @Param("status")ChatRoomStatus status);
}
