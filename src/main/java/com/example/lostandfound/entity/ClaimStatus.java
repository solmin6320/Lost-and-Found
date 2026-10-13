package com.example.lostandfound.entity;

// 주인 확인 요청 상태
public enum ClaimStatus {

    PENDING, // 대기
    APPROVED, // 승인
    REJECTED, // 거절 또는 승인 취소
    RECEIVED // 주인이 받은 상황
}
