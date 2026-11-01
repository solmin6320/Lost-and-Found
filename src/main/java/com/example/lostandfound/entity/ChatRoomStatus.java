package com.example.lostandfound.entity;

// 채팅방 상태
public enum ChatRoomStatus {

    OPEN, // 대화 중
    READ_ONLY, // 승인 취소, 읽기만
    CLOSED, // 받았어요 또는 글 삭제
    BLOCKED // 한쪽이 차단
}
