package com.example.lostandfound.repository;

import com.example.lostandfound.entity.Member;
import com.example.lostandfound.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    // 역할로 찾기
    List<Member> findAllByRole(Role role);

    // 관리자 회원 검색
    @Query(""" 
SELECT m FROM Member m 
WHERE (:role IS NULL OR m.role = :role) AND (:keyword IS NULL OR LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) 
OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')))""")
    Page<Member> searchForAdmin(@Param("keyword") String keyword, @Param("role") Role role, Pageable pageable);
}
