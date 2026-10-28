package com.example.lostandfound.service;

import com.example.lostandfound.entity.Post;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

// 매칭 정렬 점수(0 ~ 100)
@Component
public class MatchScorer {

    // 분실
    public static final int MAX_DAYS = 14;

    // 장소가 비슷하면 장소 비슷으로 보여 줌
    public static final double SIMILAR_PLACE = 0.3;


    private static final int PLACE_WEIGHT = 40;
    private static final int TITLE_WEIGHT = 25;
    private static final int CONTENT_WEIGHT = 15;
    private static final int DATE_WEIGHT = 20;

    public int score(Post lost, Post found) {
        double place = similarity(lost.getLocation(), found.getLocation());
        double title = similarity(lost.getTitle(), found.getTitle());
        double content = similarity(lost.getContent(), found.getContent());

        // 분실 뒤 빨리 주울수록 가까움
        long days = ChronoUnit.DAYS.between(lost.getLostFoundDate(), found.getLostFoundDate());
        double date = 1.0 - Math.min(Math.max(days, 0), MAX_DAYS) / (double) MAX_DAYS;

        return (int) Math.round(place * PLACE_WEIGHT + title * TITLE_WEIGHT + content * CONTENT_WEIGHT + date * DATE_WEIGHT);
    }

    // 두 글의 비슷한 정도(0 ~ 1)
    public static double similarity(String a, String b) {
        Set<String> left = bigrams(a);
        Set<String> right = bigrams(b);

        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }

        Set<String> common = new HashSet<>(left);
            common.retainAll(right);

            return 2.0 * common.size() / (left.size() + right.size());
    }

    private static Set<String> bigrams(String text) {
        Set<String> result = new HashSet<>();
            if (text == null) {
                return result;
            }
            String normalized = text.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");

            for (int i = 0; i + 1 < normalized.length(); i++) {
                result.add(normalized.substring(i, i + 2));
            }
            return result;
    }
}
