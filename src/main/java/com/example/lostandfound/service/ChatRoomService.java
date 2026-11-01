package com.example.lostandfound.service;

import com.example.lostandfound.dto.response.ChatMessageResponse;
import com.example.lostandfound.entity.*;
import com.example.lostandfound.event.ChatMessageSentEvent;
import com.example.lostandfound.event.ClaimStatusChangedEvent;
import com.example.lostandfound.event.NotificationEvent;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ClaimRepository claimRepository;
    private final MemberRepository memberRepository;
    private final PersonalInfoMasker personalInfoMasker;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationRepository notificationRepository;
    private final SimpUserRegistry simpUserRegistry;

    private static final String OPENED = "채팅방이 열렸어요. 전화번호 · 계좌번호 · 이메일은 자동으로 가려져요";
    private static final String CANCELED = "승인이 취소됐어요. 이제 대화는 읽기만 할 수 있어요";
    private static final String RECEIVED = "물건을 돌려받아 대화가 끝났어요";
    private static final String ROOM_TOPIC = "/topic/rooms";

    @EventListener
    @Transactional
    public void onClaimStatusChanged(ClaimStatusChangedEvent event) {
        switch (event.status()) {

            case APPROVED -> open(event.claimId());

            case REJECTED -> changeOpenRoom(event.claimId(), false);

            case RECEIVED -> changeOpenRoom(event.claimId(), true);

            default -> {

            }
        }
    }

    @Transactional
    public ChatMessageResponse send(Long roomId, Long memberId, String content) {
        ChatRoom room = findParticipantRoom(roomId, memberId);

        if (room.currentStatus() != ChatRoomStatus.OPEN) {
            throw new CustomException(ErrorCode.CHAT_ROOM_NOT_OPEN);
        }

        PersonalInfoMasker.Masked masked = personalInfoMasker.mask(content);

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                        .room(room)
                        .sender(memberRepository.getReferenceById(memberId))
                        .type(ChatMessageType.USER)
                        .content(masked.content())
                        .masked(masked.masked())
                .build());

        ChatMessageResponse response = ChatMessageResponse.from(message);

        eventPublisher.publishEvent(new ChatMessageSentEvent(response));
        notifyIfAway(room, memberId);

        return response;
    }



    // 방 찾고 당사자인지 확인
    private ChatRoom findParticipantRoom(Long roomId, Long memberId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (!room.isParticipant(memberId)) {
            throw new CustomException(ErrorCode.CHAT_ROOM_FORBIDDEN);
        }

        return room;
    }

    private void notifyIfAway(ChatRoom room, Long senderId) {
        Long receiverId = room.getWriter().getId().equals(senderId)
                ? room.getRequester().getId() : room.getWriter().getId();

        Claim claim = room.getClaim(); // 열린 방이라 null 아님

        if (isWatching(room.getId(), receiverId)) {
            return;
        }

        if (notificationRepository.existsByReceiverIdAndTypeAndClaimIdAndReadAtIsNull(receiverId, NotificationType.CHAT_MESSAGE, claim.getId())) {
            return;
        }

        eventPublisher.publishEvent(new NotificationEvent(
                receiverId,
                NotificationType.CHAT_MESSAGE,
                claim.getPost().getId(),
                claim.getId(),
                room.getPostTitle(),
                memberRepository.getReferenceById(senderId).getNickname()
        ));
    }

    // 상대가 이 방을 구독 중인지
    private boolean isWatching(Long roomId, Long memberId) {
        String destination = ROOM_TOPIC + roomId;
        String memberName = String.valueOf(memberId);

        return !simpUserRegistry.findSubscriptions(subscription ->
                destination.equals(subscription.getDestination()) && memberName.equals(subscription.getSession().getUser().getName())
        ).isEmpty();
    }

    // 승인 -> 방 생성
    private void open(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLAIM_NOT_FOUND));

        Long writerId = claim.getPost().getMember().getId();

        Long requesterId = claim.getClaimant().getId();

        if (chatRoomRepository.existsBetween(writerId, requesterId, ChatRoomStatus.BLOCKED)) {
            return;
        }

        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .claim(claim)
                .build());

        addSystemMessage(room, OPENED);
    }

    // 승인 취소 -> 읽기만, 받았어요
    private void changeOpenRoom(Long claimId, boolean received) {
        chatRoomRepository.findByClaimId(claimId)
                .filter(room -> room.getStatus() == ChatRoomStatus.OPEN)
                .ifPresent(room -> {
                    if (received) {
                        room.close();

                        addSystemMessage(room, RECEIVED);
                    } else {
                        room.readOnly();

                        addSystemMessage(room, CANCELED);
                    }
                });
    }

    private void addSystemMessage(ChatRoom room, String content) {
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .room(room)
                .type(ChatMessageType.SYSTEM)
                .content(content)
                .masked(false)
                .build());

        eventPublisher.publishEvent(new ChatMessageSentEvent(ChatMessageResponse.from(message)));
    }
}