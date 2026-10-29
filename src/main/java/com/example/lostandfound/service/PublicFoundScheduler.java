package com.example.lostandfound.service;

import com.example.lostandfound.config.PublicDataProperties;
import com.example.lostandfound.entity.PublicFoundItem;
import com.example.lostandfound.entity.PublicFoundSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

// 공공 습득물 하루 6번 수집(4시간마다)
@Slf4j
@Component
@RequiredArgsConstructor
public class PublicFoundScheduler {

    private final PublicDataProperties publicDataProperties;
    private final PublicFoundCollector publicFoundCollector;
    private final PublicFoundSaver publicFoundSaver;

    private static final int KEEP_DAYS = 30;

    // 서버 상관없이 한국 시간
    @Scheduled(cron = "0 0 0/4 * * *", zone = "Asia/Seoul")
    public void run() {

        // 로컬, 테스트는 실제 API를 부르지 않음
        if (!publicDataProperties.enabled()) {
            return;
        }

        for (PublicFoundSource source : PublicFoundSource.values()) {
            collect(source);
        }

        int deleted = publicFoundSaver.deleteOlderThan(LocalDate.now().minusDays(KEEP_DAYS));

        log.info("공공 습득물 {}일 지난 것 {}건 정리", KEEP_DAYS, deleted);
    }

    // 한 출처가 실패해도 다른 출처는 계속
    private void collect(PublicFoundSource source) {
        try {
            List<PublicFoundItem> added = publicFoundCollector.collect(source);

            log.info("공공 습득물 {} 새로 {}건", source, added.size());
        } catch (Exception e) {
            log.warn("공공 습득물 {} 수집 실패: {}", source, e.getMessage());
        }
    }
}
