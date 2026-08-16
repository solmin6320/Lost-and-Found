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
}
