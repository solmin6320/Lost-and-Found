package com.example.lostandfound.service;

import com.example.lostandfound.entity.PostCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.lostandfound.entity.PostCategory.*;
// 공공 습득물 분류명
@Slf4j
@Component
public class PublicCategoryMapper {

    // 대분류 이름 기준
    private static final Map<String, PostCategory> BY_TOP = Map.of(
            "지갑", WALLET,
            "휴대폰", ELECTRONICS,
            "전자기기", ELECTRONICS,
            "컴퓨터", ELECTRONICS,
            "카메라", ELECTRONICS,
            "카드", CARD,
            "의류", CLOTHES
    );

    // 모르는 이름은 한 번만 로그
    private final Set<String> logged = ConcurrentHashMap.newKeySet();

    // 지갑 -> 남성용 지갑 -> WALLET
    public PostCategory map(String categoryName) {
        if (categoryName == null) {
            return ETC;
        }

        String top = categoryName.split(">")[0].strip();

        PostCategory category = BY_TOP.get(top);

        if (category != null) {
            return category;
        }
        if (logged.add(top)) {
            log.info("공공 습득물 분류 '{}'는 ETC로 저장", top);
        }
        return ETC;
    }
}
