package com.example.lostandfound.repository;

import com.example.lostandfound.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 조회 결과가 없을 수 있기 때문에 Optional 사용
    Optional<Member> findByEmail(String email);

    // 이메일이 DB에 존재하는지 여부를 반환
    boolean existsByEmail(String email);

    // 사용자 닉네임이 DB에 존재하는지 여부를 반환
    boolean existsByNickname(String nickname);

    // 나를 제외한 다른 회원이 이 닉네임을 쓰는지 여부를 반환
    boolean existsByNicknameAndIdNot(String nickname, Long id);
}
