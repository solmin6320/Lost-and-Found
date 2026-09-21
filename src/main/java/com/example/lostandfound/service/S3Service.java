package com.example.lostandfound.service;

import com.example.lostandfound.config.AwsProperties;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
// S3 업로드, 삭제 전담
public class S3Service {

    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("jpg", "jpeg", "png", "gif");

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    // 업로드 후 객체 키를 반환
    public String upload(MultipartFile file, Long postId) {

        String storedFilename = UUID.randomUUID() + "." +
                extractExtension(file.getOriginalFilename());
        String key = "posts/" + postId + "/" + storedFilename;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(awsProperties.s3().bucket())
                .key(key)
                .contentType(file.getContentType())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return key;
    }

    // 삭제 실패는 예외로 올리지 않음
    public void delete(String key) {

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(awsProperties.s3().bucket())
                            .key(key)
                            .build());
        } catch (Exception e) {
            log.error("S3 객체 삭제 실패: {}", key, e);
        }
    }

    // 삭제는 커밋된 뒤로 미룸
    public void deleteAfterCommit(List<String> keys) {

        if (keys.isEmpty()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            keys.forEach(this::delete);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(S3Service.this::delete);
            }
        });
    }



    // 원본 파일명은 쓰지 않고 확장자만 뽑아 검증
    private String extractExtension(String originalFilename) {

        if (!StringUtils.hasText(originalFilename) ||
        !originalFilename.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_EXTENSION);
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf(".") + 1)
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_EXTENSION);
        }
        return extension;
    }
}
