package com.example.lostandfound.service;

import com.example.lostandfound.dto.request.ClaimRequest;
import com.example.lostandfound.dto.response.ClaimResponse;
import com.example.lostandfound.dto.response.MyClaimResponse;
import com.example.lostandfound.entity.Claim;
import com.example.lostandfound.entity.Post;
import com.example.lostandfound.entity.PostStatus;
import com.example.lostandfound.exception.CustomException;
import com.example.lostandfound.exception.ErrorCode;
import com.example.lostandfound.repository.ClaimRepository;
import com.example.lostandfound.repository.MemberRepository;
import com.example.lostandfound.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    // 주인 확인 요청 보내기
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ClaimResponse create(Long postId, ClaimRequest request, Long memberId) {

        Post post = findPost(postId);

        // 내 글에는 요청할 수 없음
        if (post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.CLAIM_OWN_POST);
        }

        // 게시중인 글에만
        if (post.getStatus() != PostStatus.OPEN) {
            throw new CustomException(ErrorCode.CLAIM_NOT_ACCEPTING);
        }

        // 한 글에 한 사람 한 번
        if (claimRepository.existsByPostIdAndClaimantId(postId, memberId)) {
            throw new CustomException(ErrorCode.DUPLICATE_CLAIM);
        }

        Claim claim = Claim.builder()
                .post(post)
                .claimant(memberRepository.getReferenceById(memberId))
                .answer(request.answer())
                .build();

        return ClaimResponse.from(claimRepository.save(claim));
    }

    // 받은 요청 목록
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<ClaimResponse> getReceivedClaims(Long postId, Long memberId) {

        Post post = findPost(postId);

        if (!post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return claimRepository.findAllByPostIdWithClaimant(postId).stream()
                .map(ClaimResponse::from)
                .toList();
    }

    // 내가 보낸 요청 목록
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<MyClaimResponse> getMyClaims(Long memberId, Pageable pageable) {

        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        return claimRepository.findMyClaims(memberId, page)
                .map(MyClaimResponse::from);
    }

    // 승인 - 글쓴이만
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ClaimResponse approve(Long claimId, Long memberId) {

        Claim claim = findClaim(claimId);

        Post post = postRepository.findByIdForUpdate(claim.getPost().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        checkAuthor(post, memberId);

        claim.approve();

        return ClaimResponse.from(claim);
    }

    // 거절 - 글쓴이만
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ClaimResponse reject(Long claimId, Long memberId) {

        Claim claim = findClaim(claimId);

        checkAuthor(claim.getPost(), memberId);

        claim.reject();

        return ClaimResponse.from(claim);
    }

    // 받은 쪽 - 물건을 받는 쪽(주인)
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ClaimResponse receive(Long claimId, Long memberId) {

        Claim claim = findClaim(claimId);

        if (!claim.receiverId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }

        claim.receive();

        return ClaimResponse.from(claim);
    }

    private Post findPost(Long postId) {

        return postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private Claim findClaim(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLAIM_NOT_FOUND));
    }

    // 글쓴이인지 확인
    private void checkAuthor(Post post, Long memberId) {
        if (!post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }
}
