package com.example.lostandfound.service;

import com.example.lostandfound.config.AwsProperties;
import com.example.lostandfound.dto.request.PostCreateRequest;
import com.example.lostandfound.dto.request.PostSearchCondition;
import com.example.lostandfound.dto.request.PostUpdateRequest;
import com.example.lostandfound.dto.response.PostDetailResponse;
import com.example.lostandfound.dto.response.PostListResponse;
import com.example.lostandfound.dto.response.PostResponse;
import com.example.lostandfound.dto.response.PostStatusResponse;
import com.example.lostandfound.entity.*;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.CommentRepository;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostViewService postViewService;
    private final CommentRepository commentRepository;
    private final S3Service s3Service;
    private final AwsProperties awsProperties;

    // 상세 화면에 먼저 보여줄 댓글 수
    private static final int COMMENT_PREVIEW_SIZE = 20;
    // DB 제약으로 인해 유일한 검사
    private static final int MAX_IMAGE_COUNT = 5;

    // 경로 규칙이 바뀌어도 스스로를 지키도록 선언
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PostResponse create(PostCreateRequest request, Long memberId, List<MultipartFile> images) {

        List<MultipartFile> uploadTarget = filterEmpty(images);

        // 업로드 전에 검사
        if (uploadTarget.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.EXCEEDED_IMAGE_COUNT);
        }

        // FK 값만 필요하므로 프록시만 가져옴
        Member member = memberRepository.getReferenceById(memberId);


        Post post = Post.builder()
                .member(member)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .location(request.location())
                .lostFoundDate(request.lostFoundDate())
                .build();

        // 객체 키에 postId가 들어가므로 먼저 저장
        Post postSaved = postRepository.save(post);

        for (MultipartFile file : uploadTarget) {
            postSaved.addImage(toPostImage(file, postSaved.getId()));
        }

        return PostResponse.from(postSaved);
    }

    // 비로그인도 열람 가능
    @Transactional(readOnly = true)
    public Page<PostListResponse> search(PostSearchCondition condition, Pageable pageable) {

        return postRepository.search(condition, pageable)
                .map(PostListResponse::from);
    }

    // 조회수 증가가 섞이므로 readOnly를 쓰지 않음
    @Transactional
    public PostDetailResponse getDetail(Long postId, Long memberId) {

        // 비로그인은 중복 판정이 불가해 집계하지 않음
        if (memberId != null && postViewService.isFirstView(postId, memberId)) {
            postRepository.increaseViewCount(postId);
        }

        // 증가 뒤에 조회해야 응답에 증가된 값이 담김
        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 이미지와 따로 조회
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(
                postId, PageRequest.of(0, COMMENT_PREVIEW_SIZE)
        );

        // 개수가 자명하므로 COUNT를 생략
        long totalCommentCount = (comments.size() < COMMENT_PREVIEW_SIZE) ? comments.size() : commentRepository.countByPostId(postId);


        return PostDetailResponse.from(post, comments, totalCommentCount, awsProperties.s3().baseUrl());
    }

    // 소유자 검증이 필요하므로 처리
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PostStatusResponse changeStatus(Long postId, PostStatus status, Long memberId) {

        // 게시글 상태와 작성자를 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 작성자 본인만 변경 가능
        if (!post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 전이 규칙 판단은 엔티티가 담당
        post.changeStatus(status);

        return PostStatusResponse.from(post);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public PostResponse update(Long postId, PostUpdateRequest request, List<MultipartFile> images, Long memberId) {

        // 교체 대상 이미지까지 함께 조회
        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        post.update(request.postType(), request.title(), request.content(), request.postCategory(), request.location(), request.lostFoundDate());

        replaceImages(post, filterEmpty(images), request.removeImages());


        return PostResponse.from(post);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void delete(Long postId, Long memberId) {

        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 삭제 전에 키를 확보
        List<String> keys = post.getImages().stream()
                .map(PostImage::getFilePath)
                .toList();

        // 댓글을 먼저 지움
        commentRepository.deleteByPostId(postId);
        postRepository.delete(post);

        s3Service.deleteAfterCommit(keys);
    }



    // 파일을 고르지 않아도 빈 파트가 오므로 걸러냄
    private List<MultipartFile> filterEmpty(List<MultipartFile> images) {

        if (images == null) {
            return List.of();
        }

        return images.stream()
                .filter(file -> !file.isEmpty())
                .toList();
    }

    // S3에 올린 뒤 DB에 남길 메타데이터로 변환
    private PostImage toPostImage(MultipartFile file, Long postId) {

        String key = s3Service.upload(file, postId);

        return PostImage.builder()
                .originalFilename(file.getOriginalFilename())
                .storedFilename(key.substring(key.lastIndexOf("/") + 1)) // 키의 마지막 조각
                .filePath(key) // 주소는 응답에서 조립
                .fileSize((int) file.getSize())
                .build();
    }

    // 명시적 의사표시가 있을 때만 지움
    private void replaceImages(Post post, List<MultipartFile> uploadTargets, boolean removeImages) {

        // 빈 파트만 왔고 삭제 의사도 없으면 그대로 둠
        if (uploadTargets.isEmpty() && !removeImages) {
            return;
        }

        if (uploadTargets.size() > MAX_IMAGE_COUNT) {
            throw new CustomException(ErrorCode.EXCEEDED_IMAGE_COUNT);
        }
        
        // 비우기 전에 키를 확복
        List<String> oldKeys = post.getImages().stream()
                .map(PostImage::getFilePath)
                .toList();

        post.clearImages();

        for (MultipartFile file : uploadTargets) {
            post.addImage(toPostImage(file, post.getId()));
        }

        s3Service.deleteAfterCommit(oldKeys);
    }

}
