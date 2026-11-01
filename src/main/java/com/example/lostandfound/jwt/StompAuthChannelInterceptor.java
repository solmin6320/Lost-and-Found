package com.example.lostandfound.jwt;

import com.example.lostandfound.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // 연결 때 한 번 토큰 확인
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            accessor.setUser(authenticate(accessor.getFirstNativeHeader(AUTHORIZATION_HEADER)));
        }

        return message;
    }

    // HTTP 필터와 같은 기준
    private UsernamePasswordAuthenticationToken authenticate(String bearerToken) {

        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("토큰이 없습니다");
        }

        String token = bearerToken.substring(BEARER_PREFIX.length());

        if (!jwtTokenProvider.validateToken(token) || jwtTokenProvider.isRefreshToken(token)) {
            throw new BadCredentialsException("유효하지 않은 토큰입니다");
        }

        CustomUserDetails userDetails = new CustomUserDetails(jwtTokenProvider.getMemberId(token), jwtTokenProvider.getRole(token));

        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }
}
