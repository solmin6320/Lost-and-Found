package com.example.lostandfound.security;

import com.example.lostandfound.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ChatSubscribeInterceptor implements ChannelInterceptor {

    private final ChatRoomRepository chatRoomRepository;

    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/rooms/(\\d{1,18})$");
    private static final String USER_PREFIX = "/user/";

    // 구독은 내 방과 내 칸
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        Principal user = accessor.getUser();

        if (user == null || destination == null) {
            throw new AccessDeniedException("구독할 수 없습니다");
        }

        // 내 연결로만 바뀌어 전달되는 주소
        if (destination.startsWith(USER_PREFIX)) {
            return message;
        }

        Matcher room = ROOM_TOPIC.matcher(destination);

        if (!room.matches()) {
            throw new AccessDeniedException("구독할 수 없는 주소입니다");
        }

        if (!chatRoomRepository.isParticipant(Long.valueOf(room.group(1)), Long.valueOf(user.getName()))) {
            throw new AccessDeniedException("이 대화방의 당사자가 아닙니다");
        }

        return message;
    }
}
