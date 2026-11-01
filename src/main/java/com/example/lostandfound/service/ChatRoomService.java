package com.example.lostandfound.service;

import com.example.lostandfound.dto.response.ChatMessageResponse;
import com.example.lostandfound.dto.response.ChatRoomResponse;
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
import org.springframework.data.domain.Limit;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private static final String ROOM_TOPIC = "/topic/rooms/";
    private static final int MAX_PAGE_SIZE = 50;
    private static final String BLOCKED_NOTICE = "대화가 차단됐어요. 더 이상 메시지를 보낼 수 없어요";

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

    // 내 대화방 목록
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(Long memberId) {

        List<ChatRoom> rooms = chatRoomRepository.findAllByMember(memberId);

        if (rooms.isEmpty()) {
            return List.of();
        }

        List<Long> roomIds = rooms.stream().map(ChatRoom::getId).toList();

        Map<Long, ChatMessage> lastMessages = chatMessageRepository.findLastMessages(roomIds).stream()
                .collect(Collectors.toMap(message -> message.getRoom().getId(), Function.identity()));

        Map<Long, Long> unreadCounts = chatMessageRepository.countUnread(roomIds, memberId).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return rooms.stream()
                .map(room -> ChatRoomResponse.of(room, memberId,
                        lastMessages.get(room.getId()),
                        unreadCounts.getOrDefault(room.getId(), 0L)))
                .sorted(Comparator.comparing(ChatRoomResponse::lastMessageAt).reversed())
                .toList();
    }

    // 지난 메시지
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, Long memberId, Long before, int size) {
        findParticipantRoom(roomId, memberId);

        long cursor = before == null ? Long.MAX_VALUE : before;

        int limit = Math.clamp(size, 1, MAX_PAGE_SIZE);

        return chatMessageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, cursor, Limit.of(limit)).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    // 읽음
    @Transactional
    public void markRead(Long roomId, Long memberId) {
        ChatRoom room = findParticipantRoom(roomId, memberId);

        Long lastId = chatMessageRepository.findLastId(roomId);

        if (lastId != null) {
            room.markRead(memberId, lastId);
        }

        if (room.getClaim() != null) {
            notificationRepository.markReadByClaim(memberId, NotificationType.CHAT_MESSAGE, room.getClaim().getId(), LocalDateTime.now());
        }
    }


    // 차단(당사자 누구나)
    @Transactional
    public void block(Long roomId, Long memberId) {
        ChatRoom room = findParticipantRoom(roomId, memberId);

        if (room.getStatus() == ChatRoomStatus.BLOCKED) {
            return;
        }

        room.block(memberRepository.getReferenceById(memberId));

        addSystemMessage(room, BLOCKED_NOTICE);
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