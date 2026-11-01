package com.example.lostandfound.controller;

import com.example.lostandfound.dto.request.ChatSendRequest;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.exception.ErrorResponse;
import com.example.lostandfound.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import java.security.Principal;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;


    @MessageMapping("/rooms/{roomId}")
    public void send(@DestinationVariable Long roomId,
                     @Valid @Payload ChatSendRequest request,
                     Principal principal) {

        Long memberId = Long.valueOf(principal.getName()); // 연결 때 넣은 사용자

        chatRoomService.send(roomId, memberId, request.content());

    }

    // 실패는 보낸 연결에만
    @MessageExceptionHandler(CustomException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse handleCustom(CustomException e) {
        return ErrorResponse.of(e.getErrorCode());
    }

    // 빈 메시지, 1000 초과
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse handleInvalid(MethodArgumentNotValidException e) {
        String message = Optional.ofNullable(e.getBindingResult())
                .flatMap(result -> result.getFieldErrors().stream().findFirst())
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        return ErrorResponse.of(ErrorCode.INVALID_INPUT.name(), message);
    }
}
