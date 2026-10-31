package com.example.lostandfound.service;

import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PublicFoundItem;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

@Component
public class PublicMatchScorer {

    // 이름이 비슷하면 이름 비슷으로 보여 줌
    public static final double SIMILAR_NAME = 0.3;
    private static final int TITLE_WEIGHT = 45;
    private static final int CONTENT_WEIGHT = 25;
    private static final int DATE_WEIGHT = 30;

    public int score(Post lost, PublicFoundItem item) {
        String itemText = itemText(item);
        double title = MatchScorer.similarity(lost.getTitle(), itemText);
        double content = MatchScorer.similarity(lost.getContent(), itemText);


        // 분실 뒤 빨리 주울수록 가까움
        long days = ChronoUnit.DAYS.between(lost.getLostFoundDate(), item.getFoundDate());
        double date = 1.0 - Math.min(Math.max(days, 0), MatchScorer.MAX_DAYS) / (double) MatchScorer.MAX_DAYS;

        return (int) Math.round(title * TITLE_WEIGHT + content * CONTENT_WEIGHT + date * DATE_WEIGHT);
    }

    // 내글 제목과 물품명이 비슷한지
    public static boolean similarName(Post lost, PublicFoundItem item) {
        return MatchScorer.similarity(lost.getTitle(), itemText(item)) >= SIMILAR_NAME;
    }

    // 물품명, 색상, 게시 제목을 합쳐 비교
    private static String itemText(PublicFoundItem item) {
        StringBuilder text = new StringBuilder(item.getItemName());

        if (item.getColorName() != null) {
            text.append(' ').append(item.getColorName());
        }

        if (item.getTitle() != null) {
            text.append(' ').append(item.getTitle());
        }

        return text.toString();
    }
}