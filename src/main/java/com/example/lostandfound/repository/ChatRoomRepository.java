package com.example.lostandfound.repository;

import com.example.lostandfound.entity.ChatRoom;
import com.example.lostandfound.entity.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    // 방 당사자인지(구독 확인)
    @Query("select count(r) > 0 from ChatRoom r " +
            "where r.id = :roomId " +
            "and (r.writer.id = :memberId or r.requester.id = :memberId)")
    boolean isParticipant(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    // 내 방 목록(두 사람, 요청을 함께)
    @Query("select r from ChatRoom r " +
            "join fetch r.writer " +
            "join fetch r.requester " +
            "left join fetch r.claim " +
            "where r.writer.id = :memberId or r.requester.id = :memberId")
    List<ChatRoom> findAllByMember(@Param("memberId") Long memberId);

}
