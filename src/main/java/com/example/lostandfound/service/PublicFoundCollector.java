package com.example.lostandfound.service;

import com.example.lostandfound.client.PublicFoundClient;
import com.example.lostandfound.client.PublicFoundPage;
import com.example.lostandfound.entity.PublicFoundItem;
import com.example.lostandfound.entity.PublicFoundSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 한 출처의 최근 습득물을 끝까지 받음
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicFoundCollector {

    private final PublicFoundClient publicFoundClient;
    private final PublicFoundSaver publicFoundSaver;

    private static final int WINDOW_DAYS = 14;
    private static final int MAX_PAGES = 500; // 트래픽 보호
    private static final int ROWS = 100;


    // 쪽마다 저장
    public List<PublicFoundItem> collect(PublicFoundSource source) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(WINDOW_DAYS);

        List<PublicFoundItem> added = new ArrayList<>();

        for (int pageNo = 1; pageNo <= MAX_PAGES; pageNo++) {
            PublicFoundPage page = publicFoundClient.fetch(source, from, to, pageNo, ROWS);

            added.addAll(publicFoundSaver.savePage(source, page.rows()));

            if (page.rows().isEmpty() || pageNo * ROWS >= page.totalCount()) {
                return added;
            }
        }

        log.warn("공공 습득물 {} 수집이 {}쪽을 넘어 멈춤", source, MAX_PAGES);
        return added;
    }
}
