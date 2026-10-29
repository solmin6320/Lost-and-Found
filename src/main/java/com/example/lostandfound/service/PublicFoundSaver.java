package com.example.lostandfound.service;

import com.example.lostandfound.client.PublicFoundPage;
import com.example.lostandfound.entity.PublicFoundItem;
import com.example.lostandfound.entity.PublicFoundSource;
import com.example.lostandfound.repository.PublicFoundItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 받은 한 쪽을 저장, 있으면 갱신 없으면 추가
@Service
@RequiredArgsConstructor
public class PublicFoundSaver {

    private final PublicFoundItemRepository publicFoundItemRepository;
    private final PublicCategoryMapper categoryMapper;

    // 새로 추가된 것만 돌려줌
    @Transactional
    public List<PublicFoundItem> savePage(PublicFoundSource source, List<PublicFoundPage.Row> rows) {
        List<PublicFoundItem> added = new ArrayList<>();

        for (PublicFoundPage.Row row : rows) {

            // NOT NULL 칸이 비면 건너뜀
            if (row.atcId() == null || row.itemName() == null || row.foundDate() == null) {
                continue;
            }

            Optional<PublicFoundItem> saved = publicFoundItemRepository.findBySourceAndExternalId(source, cut(row.externalId(), 30));

            if (saved.isPresent()) {
                saved.get().refresh(cut(row.itemName(), 200),
                        categoryMapper.map(row.categoryName()),
                        cut(row.categoryName(), 100),
                        cut(row.colorName(), 100),
                        row.foundDate(),
                        cut(row.storagePlace(), 100),
                        imageUrlOf(row),
                        cut(row.title(), 300));
                continue;
            }

            added.add(publicFoundItemRepository.save(PublicFoundItem.builder()
                            .source(source)
                            .externalId(cut(row.externalId(), 30))
                            .itemName(cut(row.itemName(), 200))
                            .category(categoryMapper.map(row.categoryName()))
                            .categoryName(cut(row.categoryName(), 100))
                            .colorName(cut(row.colorName(), 100))
                            .foundDate(row.foundDate())
                            .storagePlace(cut(row.storagePlace(), 100))
                            .imageUrl(imageUrlOf(row))
                            .title(cut(row.title(), 300))
                            .build()));
        }
        return added;
    }

    // 칸 길이를 넘으면 잘라서 저장 실패를 막음
    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    // 주소는 자르면 깨지므로 길면 사진 없이
    private static String imageUrlOf(PublicFoundPage.Row row) {
        String url = row.imageUrl();

        return url == null || url.length() > 300 ? null : url;
    }
}
