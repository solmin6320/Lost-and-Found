package com.example.lostandfound.repository;

import com.example.lostandfound.entity.PublicFoundItem;
import com.example.lostandfound.entity.PublicFoundSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PublicFoundItemRepository extends JpaRepository<PublicFoundItem, Long> {

    // 이미 받은 습득물인지
    Optional<PublicFoundItem> findBySourceAndExternalId(PublicFoundSource source, String externalId);

    // 습득일이 오래된 것은 정리
    @Modifying(clearAutomatically = true)
    @Query("delete from PublicFoundItem i where i.foundDate < :before")
    int deleteOlderThan(@Param("before") LocalDate before);

}
