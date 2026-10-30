package com.example.lostandfound.entity;

// 알림 종류
public enum NotificationType {
    CLAIM_REQUESTED, // 내 글에 주인 확인 요청이 옴
    CLAIM_APPROVED, // 내 요청이 승인됨
    CLAIM_REJECTED, //내 요청이 거절됨
    MATCH_SUGGESTED, // 매칭 후보가 생김
    PUBLIC_MATCH_SUGGESTED, // 경찰서, 기관 보관 습득물
    CHAT_MESSAGE // 새 채팅 메시지
}
